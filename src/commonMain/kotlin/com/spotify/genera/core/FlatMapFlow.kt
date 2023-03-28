package com.spotify.genera.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// TODO: is this really a good idea? What other FlatMap options are there?
// TODO: should this maybe be in some coroutines-specific sub-package. Or maybe not, since it's using 'common' stuff.
public class FlatMapFlow<In, Out>(
        private val parentScope: CoroutineScope, // should probably be supplied as some kind of 'loop scope'.
        private val mapper: (In) -> Flow<Out>,
) : Connectable<In, Out> {
    override fun connect(output: Consumer<Out>): Connection<In> = FlatMapFlowConnection(mapper, output, parentScope)

    private class FlatMapFlowConnection<In, Out>(
            private val mapper: (In) -> Flow<Out>,
            private val consumer: Consumer<Out>,
            parentScope: CoroutineScope,
    ) : Connection<In> {
        val scope = CoroutineScope(parentScope.coroutineContext + Job(parent = parentScope.coroutineContext[Job]))

        override fun consume(data: In) {
            check(scope.isActive) { "FlatMapFlow has been disposed" }

            scope.launch {
                mapper.invoke(data).collect { value -> consumer.consume(value) }
            }
        }

        override fun dispose() {
            scope.cancel(CancellationException("FlatMapFlow disposed"))
        }
    }
}
