package com.spotify.genera.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import org.awaitility.Awaitility.await
import org.awaitility.Awaitility.given
import org.awaitility.kotlin.withAlias
import java.time.Duration
import java.util.concurrent.CountDownLatch

@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleManagerTest : ShouldSpec({
    lateinit var manager: LifecycleManager<String>
    lateinit var scope: TestScope
    lateinit var loopFactoryConsumer: Consumer<String>
    lateinit var receivedEvents: MutableList<String>

    var exceptionInThread: Throwable? = null
    lateinit var t: Thread

    context("starting the loop") {
        lateinit var pauseStarting: CountDownLatch

        beforeEach {
            receivedEvents = mutableListOf()
            pauseStarting = CountDownLatch(1)

            scope = TestScope()
            manager = LifecycleManager(scope) { consumer ->
                loopFactoryConsumer = consumer

                pauseStarting.await()

                IgnoreDispose { receivedEvents.add(it) }
            }

            exceptionInThread = null
            t = Thread {
                try {
                    manager.start()
                } catch (e: Throwable) {
                    exceptionInThread = e
                }
            }
        }

        afterEach {
            // capture exceptions from thread, if any
            pauseStarting.countDown()
            t.join()

            exceptionInThread?.let {
                throw it
            }
        }

        should("not allow external events before starting") {
            shouldThrow<IllegalStateException> { manager.consume("oh no!") }
        }

        should("not forward external events while starting") {
            // given a starting manager that hasn't yet finished starting (waiting on the latch)
            t.start()

            // when a 'consume' request is accepted
            given()
                .withAlias("wait for startup")
                .ignoreException(IllegalStateException::class.java)
                .await()
                .atMost(Duration.ofSeconds(5))
                .until {
                    manager.consume("starting up")
                    true
                }

            // then the data isn't forwarded - this may be flaky: if so, a failure is a true positive, meaning that
            // the sleep may need to be a bit longer to always detect a bug.
            Thread.sleep(300)

            receivedEvents.size shouldBe 0
        }

        should("not forward events from loop while starting") {
            t.start()

            given()
                .withAlias("wait for startup")
                .ignoreException(IllegalStateException::class.java)
                .await()
                .atMost(Duration.ofSeconds(5))
                .until {
                    manager.consume("starting up")
                    true
                }

            // now, there's one event in the queue, we're adding another one from 'inside' the loop,
            // using the consumer that was used to construct the loop.

            loopFactoryConsumer.consume("this should not be forwarded")

            // then the data isn't forwarded - this may be flaky: if so, a failure is a true positive, meaning that
            // the sleep may need to be a bit longer to always detect a bug.
            Thread.sleep(300)

            receivedEvents.size shouldBe 0
        }

        should("fail if start() called while starting") {
            t.start()

            given()
                .withAlias("wait for startup")
                .ignoreException(IllegalStateException::class.java)
                .await()
                .atMost(Duration.ofSeconds(5))
                .until {
                    manager.consume("starting up")
                    true
                }

            shouldThrow<IllegalStateException> { manager.start() }
        }

        should("send queued events when started") {
            t.start()

            given()
                .withAlias("wait for startup")
                .ignoreException(IllegalStateException::class.java)
                .await()
                .atMost(Duration.ofSeconds(5))
                .until {
                    manager.consume("starting up")
                    true
                }

            loopFactoryConsumer.consume("sent from the loop")

            pauseStarting.countDown()

            await().atMost(Duration.ofSeconds(1)).until {
                receivedEvents.containsAll(listOf("starting up", "sent from the loop"))
            }
        }
    }

    context("stopping the loop") {
        lateinit var stopping: CountDownLatch
        lateinit var pauseStopping: CountDownLatch

        beforeEach {
            receivedEvents = mutableListOf()
            stopping = CountDownLatch(1)
            pauseStopping = CountDownLatch(1)

            scope = TestScope()
            manager = LifecycleManager(scope) { consumer ->
                loopFactoryConsumer = consumer

                object : Connection<String> {
                    override fun consume(data: String) {
                        receivedEvents.add(data)
                    }

                    override fun dispose() {
                        pauseStopping.await()
                    }
                }
            }

            exceptionInThread = null
            t = Thread {
                try {
                    stopping.countDown()
                    manager.dispose()
                } catch (e: Throwable) {
                    exceptionInThread = e
                }
            }
        }

        afterEach {
            // capture exceptions from thread, if any
            pauseStopping.countDown()
            t.join()

            exceptionInThread?.let {
                throw it
            }
        }

        should("drop external events while stopping") {
            manager.start()

            manager.consume("show this")

            // start disposing thread
            t.start()

            // wait for the thread to 'start stopping', and then a little bit more to reduce the risk of flakiness
            stopping.await()
            Thread.sleep(50)

            manager.consume("don't show this")

            pauseStopping.countDown()

            t.join()
            receivedEvents.shouldContainExactly("show this")
        }

        should("drop loop events while stopping") {
            manager.start()

            loopFactoryConsumer.consume("show this")

            // start disposing thread
            t.start()

            // wait for the thread to 'start stopping', and then a little bit more to reduce the risk of flakiness
            stopping.await()
            Thread.sleep(50)

            loopFactoryConsumer.consume("don't show this")

            pauseStopping.countDown()

            t.join()
            receivedEvents.shouldContainExactly("show this")
        }
    }

    context("the loop is disposed") {
        var connectionDisposed = false

        beforeEach {
            receivedEvents = mutableListOf()

            scope = TestScope()
            manager = LifecycleManager(scope) { consumer ->
                loopFactoryConsumer = consumer

                object : Connection<String> {
                    override fun consume(data: String) {
                        receivedEvents.add(data)
                    }

                    override fun dispose() {
                        connectionDisposed = true
                    }
                }
            }
        }

        should("throw for external events when stopped") {
            manager.start()
            manager.dispose()

            shouldThrow<IllegalStateException> { manager.consume("oh no!") }
        }

        should("cancel the coroutine scope when disposed") {
            manager.dispose()

            scope.isActive shouldBe false
        }

        should("dispose the loop connection when disposed") {
            manager.start()
            manager.dispose()

            connectionDisposed shouldBe true
        }
    }
})
