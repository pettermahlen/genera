package com.spotify.genera.core

public class MapItems<In, Out>(
    private val mapper: (In) -> Out,
) : Connectable<In, Out> {
    override fun connect(output: Consumer<Out>): Connection<In> =
        IgnoreDispose { data -> output.consume(mapper.invoke(data)) }
}