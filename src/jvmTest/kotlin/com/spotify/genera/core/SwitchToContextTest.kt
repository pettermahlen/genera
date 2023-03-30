package com.spotify.genera.core

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.contain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.newSingleThreadContext
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import java.time.Duration

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class SwitchToContextTest : ShouldSpec({
    lateinit var switchToContext: SwitchToContext<String>
    val scope = CoroutineScope(Dispatchers.Default)

    context("execute in single threaded context") {
        beforeEach {
            switchToContext = SwitchToContext(scope) { count -> newSingleThreadContext("this is my thread $count") }
        }

        should("execute next on new context") {
            var thread: Thread? = null

            switchToContext.connect {
                thread = Thread.currentThread()
            }.consume("hey")

            // ensure that the concurrent code gets some time to run
            await atMost Duration.ofSeconds(5) untilNotNull { thread?.name }

            thread!!.name!!.should(contain("this is my thread "))
        }

        should("increment counter on each connection attempt") {
            var thread1: Thread? = null
            var thread2: Thread? = null

            switchToContext.connect {
                thread1 = Thread.currentThread()
            }.consume("hey")
            switchToContext.connect {
                thread2 = Thread.currentThread()
            }.consume("hey")

            // ensure that the concurrent code gets some time to run
            await atMost Duration.ofSeconds(5) untilNotNull { thread2?.name }

            thread1!!.name!!.should(contain("this is my thread 0"))
            thread2!!.name!!.should(contain("this is my thread 1"))
        }
    }

    should("close dispatcher on dispose") {
        val dispatcher = newSingleThreadContext("hi")

        switchToContext = SwitchToContext(scope) { dispatcher }

        val connection = switchToContext.connect {}

        connection.dispose()

        dispatcher.isActive shouldBe false
    }
})
