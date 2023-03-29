package com.spotify.genera.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class StateHolderTest : ShouldSpec({
    val holder: StateHolder<String, List<String>> = StateHolder(
        { model: List<String>, event: String -> model + event },
        emptyList(),
    )

    should("update the model") {
        val models = mutableListOf<List<String>>()

        val connection = holder.connect { data -> models.add(data) }

        connection.consume("a string")

        models shouldBe listOf(listOf("a string"))
    }

    should("throw after disposed") {
        val connection = holder.connect { }

        connection.dispose()

        val thrown = shouldThrow<IllegalStateException> { connection.consume("hi") }

        thrown.message.shouldContain("disposed")
    }

    // TODO:
    // - concurrency safeness? Figure out how to do that, it could be a 'mixin' connectable, too.
})