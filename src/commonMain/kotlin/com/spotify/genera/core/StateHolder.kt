package com.spotify.genera.core

import kotlin.jvm.Volatile

public class StateHolder<Event, Model>(
    private val update: (Model, Event) -> Model,
    private val initialState: Model,
): Connectable<Event, Model> {
    override fun connect(output: Consumer<Model>): Connection<Event> = StateHolderConnection(update, initialState, output)
}

private class StateHolderConnection<Event, Model>(
    private val update: (Model, Event) -> Model,
    initialState: Model,
    private val modelObserver: Consumer<Model>,
): Connection<Event> {
    private var state: Model = initialState

    // TODO: is this really right? It's probably a good idea to use some other mechanism that provides stronger
    //       guarantees that once dispose returns, no more data can be produced.
    @Volatile
    private var disposed = false

    override fun consume(data: Event) {
        check(!disposed) { "Cannot handle events when disposed."}

        state = update(state, data)

        modelObserver.consume(state)
    }

    override fun dispose() {
        disposed = true
    }
}
