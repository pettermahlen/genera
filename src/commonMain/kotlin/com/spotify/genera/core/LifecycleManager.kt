package com.spotify.genera.core

import kotlinx.coroutines.CoroutineScope

// TODO: is this core or mobius-specific?
// it's like a lock in the chain - the thing that allows for connecting different links together.
public class LifecycleManager<Event>(scope: CoroutineScope) : Consumer<Event>, Connectable<Event, Event> {
    override fun connect(output: Consumer<Event>): Connection<Event> {
        TODO("Not yet implemented")
    }

    override fun consume(data: Event) {
        TODO("Not yet implemented")
    }

}
