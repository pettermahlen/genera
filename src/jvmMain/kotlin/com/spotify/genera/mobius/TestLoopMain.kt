package com.spotify.genera.mobius

import com.spotify.genera.core.Connectable
import com.spotify.genera.core.Connection
import com.spotify.genera.core.Consumer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.system.exitProcess

public data class MyModel(
    val firstEvent: String?,
    val lastEvent: String?,
)

public sealed class MyEffect {
    public data class Println(val line: String) : MyEffect()
}

public fun main() {
    val loop = MobiusLoop(
        "test",
        ::update,
        { effect ->
            println(effect.toString())
            emptyFlow()
        },
        TextUi(),
        MyModel(null, null)
    )

    loop.observe {
        if (it == MyModel("quit", "quit")) {
            exitProcess(0)
        }
    }

    loop.dispatchEvent("hi")

    while (true) {
        // busy loop. really ugly, yah. :)
    }
}

private fun update(model: MyModel, event: String): Next<MyModel, MyEffect> =
    if (model.firstEvent == null) {
        Next(MyModel(event, event), emptySet())
    } else {
        Next(model.copy(lastEvent = event), setOf(MyEffect.Println("forgot event ${model.lastEvent}")))
    }


private class TextUi: Connectable<MyModel, String> {
    override fun connect(output: Consumer<String>): Connection<MyModel> = TextUiConnection(output)
}

private class TextUiConnection(
    private val commandConsumer: Consumer<String>,
) : Connection<MyModel> {
    override fun consume(data: MyModel) {
        println("got model: $data")
        print("command:")
        commandConsumer.consume(readln())
    }

    override fun dispose() {
        println("byebye")
    }
}