package com.spotify.genera.core

public fun interface Consumer<T> {
    public fun consume(data: T): Unit
}

public interface Disposable {
    public fun dispose(): Unit
}

public interface Connection<T> : Disposable, Consumer<T>

public fun interface Connectable<In, Out> {
    public fun connect(output: Consumer<Out>): Connection<In>
}

public class TooManyConnections : Exception()