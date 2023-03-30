package com.spotify.genera.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly

class ObservableItemsTest : ShouldSpec({
    lateinit var observable: ObservableItems<String>

    beforeEach {
        observable = ObservableItems()
    }

    should("forward new items to observers") {
        val observed = mutableListOf<String>()
        val connection = observable.connect {}

        connection.observe { observed.add(it) }

        connection.consume("hey ho")
        connection.consume("to the mountain we go")

        observed.shouldContainExactly("hey ho", "to the mountain we go")
    }

    should("allow adding observers"){
        val observed1 = mutableListOf<String>()
        val observed2 = mutableListOf<String>()

        val connection = observable.connect {}

        connection.observe { observed1.add(it) }
        connection.consume("hey ho")

        connection.observe { observed2.add(it) }
        connection.consume("to the mountain we go")

        observed1.shouldContainExactly("hey ho", "to the mountain we go")
        observed2.shouldContainExactly("to the mountain we go")
    }

    should("not forward items to disposed observers") {
        val observed1 = mutableListOf<String>()
        val observed2 = mutableListOf<String>()

        val connection = observable.connect {}

        connection.observe { observed1.add(it) }
        val observer2 = connection.observe { observed2.add(it) }

        connection.consume("hey ho")

        observer2.dispose()

        connection.consume("to the mountain we go")

        observed1.shouldContainExactly("hey ho", "to the mountain we go")
        observed2.shouldContainExactly("hey ho")
    }

    should("not allow new items after dispose") {
        val connection = observable.connect {}

        connection.dispose()

        shouldThrow<IllegalStateException> { connection.consume("hi") }
    }

    should("not allow new observers after dispose") {
        val connection = observable.connect {}

        connection.dispose()

        shouldThrow<IllegalStateException> { connection.observe {} }
    }
})
