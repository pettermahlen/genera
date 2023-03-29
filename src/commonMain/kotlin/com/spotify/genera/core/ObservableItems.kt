package com.spotify.genera.core

public class ObservableItems<Data> : Connectable<Data, Data> {


    override fun connect(output: Consumer<Data>): Connection<Data> {
        TODO("Not yet implemented")
    }

    public fun observe(consumer: Consumer<Data>): Disposable {
        TODO()
    }
}
