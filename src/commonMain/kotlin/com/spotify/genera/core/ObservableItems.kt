package com.spotify.genera.core

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

public class ObservableItems<Data> : Connectable<Data, Data> {
    override fun connect(output: Consumer<Data>): ObservableConnection<Data> = ObservableConnection()
}

public class ObservableConnection<Data>: Connection<Data> {
    private val observers = atomic(setOf<Consumer<Data>>())
    private val disposed = atomic(false)

    override fun consume(data: Data) {
        check(!disposed.value)

        observers.value.forEach {
            it.consume(data)
        }
    }

    override fun dispose() {
        disposed.value = true
    }

    public fun observe(consumer: Consumer<Data>): Disposable {
        check(!disposed.value)

        observers.update { current ->
            current.plus(consumer)
        }

        return Disposable {
            observers.update { current ->
                current.minus(consumer)
            }
        }
    }
}
