package com.spotify.genera.core

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly


class MapTest : ShouldSpec({
    val map = Map<String, Int> { s -> s.length }

    should("apply mapper to inputs") {
        val results = mutableListOf<Int>()

        val connection = map.connect { results.add(it) }

        connection.consume("hi there")

        results.shouldContainExactly(8)
    }
})