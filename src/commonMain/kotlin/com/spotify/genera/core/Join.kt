package com.spotify.genera.core

// TODO: is this needed at all, given that a connection is a consumer already, and the same connection can just
//       be passed into multiple connectables?
public class Join<Data> : Connectable<Data, Data> {

    override fun connect(output: Consumer<Data>): JoinConnection<Data> {
        TODO("Not yet implemented")
    }
}

public class JoinConnection<Data> : Connection<Data> {
    override fun consume(data: Data) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

}