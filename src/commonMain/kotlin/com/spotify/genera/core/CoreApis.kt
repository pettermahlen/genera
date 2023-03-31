package com.spotify.genera.core

// TODO: figure out variance here so that the LifecycleManager thing can be built like krka's mobius solution.
public fun interface Consumer<in T> {
    public fun consume(data: T): Unit
}

public fun interface Disposable {
    public fun dispose(): Unit
}

public interface Connection<in T> : Disposable, Consumer<T>

public fun interface Connectable<In, Out> {
    public fun connect(output: Consumer<Out>): Connection<In>
}

public class TooManyConnections : Exception()