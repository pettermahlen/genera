package com.spotify.genera.core

import kotlinx.coroutines.CloseableCoroutineDispatcher

public class SwitchToContext<Data>(
    private val contextFactory: (Int) -> CloseableCoroutineDispatcher,
) : Connectable<Data, Data> {
    override fun connect(output: Consumer<Data>): Connection<Data> {
        TODO("Not yet implemented")
    }
}
