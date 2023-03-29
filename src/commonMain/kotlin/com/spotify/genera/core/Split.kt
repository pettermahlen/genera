package com.spotify.genera.core

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

public class Split<Data> : Connectable<Data, Data> {
    public override fun connect(output: Consumer<Data>): SplitConnection<Data> = SplitConnection(output)
}

public class SplitConnection<Data>(
    consumer: Consumer<Data>
) : Connection<Data>, Connectable<Data, Data> {

    private enum class State {
        INITIALISING,
        ACTIVE,
        DISPOSED,
    }

    private val state = atomic(State.INITIALISING)
    private val mergedConsumer = MergedConsumer(consumer)

    override fun consume(data: Data) {
        state.update { state ->
            check(state == State.INITIALISING || state == State.ACTIVE)

            State.ACTIVE
        }

        mergedConsumer.consume(data)
    }

    override fun dispose() {
        state.getAndSet(State.DISPOSED)
    }

    override fun connect(output: Consumer<Data>): Connection<Data> {
        // using the atomic to guard access to the mutable MergedConsumer.
        state.update { state ->
            check(state == State.INITIALISING)

            mergedConsumer.addConsumer(output)

            state
        }

        return this
    }
}

private class MergedConsumer<Data>(consumer: Consumer<Data>) : Consumer<Data> {
    private val consumers = mutableListOf(consumer)

    fun addConsumer(consumer: Consumer<Data>) {
        consumers.add(consumer)
    }

    override fun consume(data: Data) {
        consumers.forEach { it.consume(data) }
    }
}