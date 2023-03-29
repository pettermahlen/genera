package com.spotify.genera.core


public class Split<Data> : Connectable<Data, Data> {
    public override fun connect(output: Consumer<Data>): SplitConnection<Data> {
        TODO("Not yet implemented")
    }
}

public class SplitConnection<Data>: Connection<Data>, Connectable<Data, Data> {
    override fun consume(data: Data) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun connect(output: Consumer<Data>): Connection<Data> {
        TODO("Not yet implemented")
    }
}