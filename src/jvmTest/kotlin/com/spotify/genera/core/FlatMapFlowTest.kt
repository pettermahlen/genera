package com.spotify.genera.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle


@OptIn(ExperimentalCoroutinesApi::class)
class FlatMapFlowTest : ShouldSpec({
    val channels = mutableMapOf<String, Channel<Int>>()
    val received = mutableSetOf<Int>()

    lateinit var testScope: TestScope

    lateinit var flatMapFlow: FlatMapFlow<String, Int>


    lateinit var connection: Connection<String>

    beforeEach {
        channels.clear()
        received.clear()
        testScope = TestScope()

        flatMapFlow = FlatMapFlow<String, Int>(testScope) { input ->
            val channel = Channel<Int>(100)

            channels[input] = channel

            channel.consumeAsFlow()
        }

        connection = flatMapFlow.connect { data -> received.add(data) }
    }

    context("item mapping") {
        should("forward mapped items in order") {
            connection.consume("hi")

            testScope.advanceUntilIdle()

            val channel = channels["hi"]!!

            channel.send(823)
            channel.send(87234)

            testScope.advanceUntilIdle()

            received.shouldContainExactly(823, 87234)

            connection.dispose()
        }

        should("support multiple parallel maps") {
            connection.consume("hi")
            connection.consume("ho")

            testScope.advanceUntilIdle()

            val hi = channels["hi"]!!
            val ho = channels["ho"]!!

            ho.send(14)
            ho.send(91)
            hi.send(3248)
            ho.send(12)
            hi.send(17)

            testScope.advanceUntilIdle()

            // items should be received in the order in which they were emitted by the respective mapper -
            // but we make no guarantees about order between mappers
            received.shouldContainInOrder(14, 91, 12)
            received.shouldContainInOrder(3248, 17)
            received.size shouldBe 5
        }
    }

    context("disposal") {
        should("not support sending once disposed") {
            connection.dispose()

            val thrown = shouldThrow<IllegalStateException> { connection.consume("hi") }

            thrown.message.shouldContain("disposed")
        }

        should("not emit things before dispose") {
            connection.consume("a thing")

            testScope.advanceUntilIdle()

            val channel = channels["a thing"]!!

            channel.send(129)

            testScope.advanceUntilIdle()

            connection.dispose()

            channel.send(9823)

            testScope.advanceUntilIdle()

            received.shouldContainExactly(129)
        }
    }

    // TODO: concurrency/semantics tests, such as
    // - channel is closed
    // - multiple consumes: all data should be received; should use different channels in this test for that.
})
