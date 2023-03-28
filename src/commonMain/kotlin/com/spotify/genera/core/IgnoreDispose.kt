package com.spotify.genera.core

public class IgnoreDispose<T>(
    private val consumer: Consumer<T>,
): Connection<T> {
    override fun consume(data: T): Unit = consumer.consume(data)

    override fun dispose() {
        // ignoring
    }
}