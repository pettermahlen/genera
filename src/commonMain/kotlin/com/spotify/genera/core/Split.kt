package com.spotify.genera.core

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

// TODO: maybe call this Fanout? Split is probably fine.
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
    private val splittingConsumer = SplittingConsumer(consumer)

    override fun consume(data: Data) {
        state.update { state ->
            check(state == State.INITIALISING || state == State.ACTIVE)

            State.ACTIVE
        }

        splittingConsumer.consume(data)
    }

    override fun dispose() {
        state.getAndSet(State.DISPOSED)
    }

    override fun connect(output: Consumer<Data>): Connection<Data> {
        check(state.value == State.INITIALISING)

        splittingConsumer.addConsumer(output)

        return this
    }
}

private class SplittingConsumer<Data>(consumer: Consumer<Data>) : Consumer<Data> {
    private val consumers = atomic(listOf(consumer))

    fun addConsumer(consumer: Consumer<Data>) {
        consumers.update { current -> current.plus(consumer) }
    }

    override fun consume(data: Data) {
        consumers.value.forEach { it.consume(data) }
    }
}