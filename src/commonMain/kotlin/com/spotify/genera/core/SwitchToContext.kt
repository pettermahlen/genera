@file:OptIn(ExperimentalCoroutinesApi::class)

package com.spotify.genera.core

import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

public class SwitchToContext<Data>(
    private val scope: CoroutineScope,
    private val dispatcherFactory: (Int) -> CloseableCoroutineDispatcher,
) : Connectable<Data, Data> {
    private var count = 0

    override fun connect(output: Consumer<Data>): Connection<Data> = SwitchToContextConnection(scope, dispatcherFactory.invoke(count++), output)
}

private class SwitchToContextConnection<Data>(
    private val scope: CoroutineScope,
    private val dispatcher: CloseableCoroutineDispatcher,
    private val consumer: Consumer<Data>,
) : Connection<Data> {
    override fun consume(data: Data) {
        scope.launch(dispatcher) {
            consumer.consume(data)
        }
    }

    override fun dispose() {
        dispatcher.close()
    }
}