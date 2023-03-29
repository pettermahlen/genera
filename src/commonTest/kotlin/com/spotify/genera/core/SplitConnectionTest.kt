package com.spotify.genera.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeSameInstanceAs

class SplitConnectionTest : ShouldSpec({
    lateinit var split: Split<String>

    beforeEach {
        split = Split()
    }

    should("split up events to all connected downstreams") {
        val items1 = mutableListOf<String>()
        val items2 = mutableListOf<String>()

        val connection = split
            .connect { items1.add(it) }
            .connect { items2.add(it) }

        connection.consume("hi there")
        connection.consume("again")

        items1.shouldContainExactly("hi there", "again")
        items2.shouldContainExactly("hi there", "again")
    }

    should("return itself on multiple connects") {
        val connection = split.connect { }

        val connection2 = connection.connect { }

        connection shouldBeSameInstanceAs connection2
    }

    should("fail if connections are attempted after first event received") {
        val connection = split.connect { }

        connection.consume("something")

        shouldThrow<IllegalStateException> { connection.connect {} }
    }

    should("not support connections after dispose") {
        val connection = split.connect {}

        connection.dispose()

        shouldThrow<IllegalStateException> { connection.connect {} }
    }

    should("not support consumption after dispose") {
        val connection = split.connect {}

        connection.dispose()

        shouldThrow<IllegalStateException> { connection.consume("hi") }
    }
})
