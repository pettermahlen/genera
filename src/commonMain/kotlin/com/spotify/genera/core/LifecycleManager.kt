package com.spotify.genera.core

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

// TODO: is this core or mobius-specific?
// it's like a lock in the chain - the thing that allows for connecting different links together.
public class LifecycleManager<Event>(
    // TODO: the scope is a little unclear. It kind of fits here because it should be managed by this guy. But it
    //       would be a lot nicer if the scope was managed in such a way that it's hard for the loop factory to break
    //       structured concurrency.
    private val scope: CoroutineScope,
    private val loopFactory: (Consumer<Event>) -> Connection<Event>,
) : Consumer<Event>, Disposable {

    // this class has the following states:
    // 1. Initial/inactive. It's illegal to pass in data to consume(). This is indicated by connection == INITIAL.
    // 2. Starting. Messages sent to consume() will be queued. At this stage, connection == queuing.
    // 3. Active. Messages are processed normally. At this stage, connection == the live connection.
    // 4. Stopping. Messages are discarded. At this stage, connection == DISCARDING.
    // 5. Stopped. It's illegal to pass in data to consume(). This is indicated by connection == DISPOSED.

    private val queuing = Queuing()
    private val connection: AtomicRef<Connection<Event>> = atomic(initial())

    override fun consume(data: Event) {
        connection.value.consume(data)
    }

    override fun dispose() {
        if (connection.value == DISPOSED) {
            return
        }

        val previousConnection = connection.getAndSet(discarding())

        if (previousConnection == DISCARDING) {
            // some other thread is currently running dispose, let's back off
            return
        }

        // ok, we're it. let's stop.
        previousConnection.dispose()
        scope.cancel(CancellationException("LifecycleManager disposed"))

        connection.value = disposed()
    }

    public fun start() {
        check(connection.compareAndSet(initial(), queuing))

        val eventConnection = loopFactory.invoke(this)

        connection.compareAndSet(queuing, eventConnection)

        queuing.drainQueueIfActive()
    }


    private inner class Queuing : Connection<Event> {
        val queue = atomic(listOf<Event>())
        override fun consume(data: Event) {
            queue.update { current -> current.plus(data) }

            drainQueueIfActive()
        }

        override fun dispose() {
            // clear the queue for better GC ergonomics
            queue.value = emptyList()
        }

        fun drainQueueIfActive() {
            val currentConnection = connection.value

            // if we're not queuing currently, we should drain the queue.
            if (currentConnection != queuing) {
                val queued = queue.getAndSet(emptyList())

                queued.forEach {
                    currentConnection.consume(it)
                }
            }
        }
    }
}


@Suppress("UNCHECKED_CAST")
private fun <T> initial(): Connection<T> = INITIAL as Connection<T>

private val INITIAL = object : Connection<Any> {
    override fun consume(data: Any) {
        throw IllegalStateException("LifecycleManager not started when event $data arrived")
    }

    override fun dispose() {
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> disposed(): Connection<T> = DISPOSED as Connection<T>

private val DISPOSED = object : Connection<Any> {
    override fun consume(data: Any) {
        throw IllegalStateException("Lifecycle Manager disposed when event $data arrived")
    }

    override fun dispose() {
        // empty
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> discarding(): Connection<T> = DISCARDING as Connection<T>

private val DISCARDING = object : Connection<Any> {
    override fun consume(data: Any) {
        // empty
    }

    override fun dispose() {
        // empty
    }
}