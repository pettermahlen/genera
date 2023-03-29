@file:OptIn(ExperimentalCoroutinesApi::class)

package com.spotify.genera.mobius

import com.spotify.genera.core.Connectable
import com.spotify.genera.core.Consumer
import com.spotify.genera.core.Disposable
import com.spotify.genera.core.FlatMapFlow
import com.spotify.genera.core.Join
import com.spotify.genera.core.LifecycleManager
import com.spotify.genera.core.MapItems
import com.spotify.genera.core.ObservableItems
import com.spotify.genera.core.Split
import com.spotify.genera.core.StateHolder
import com.spotify.genera.core.SwitchToContext
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

public expect fun newSingleThreadRunner(name: String): CloseableCoroutineDispatcher

public class MobiusLoop<Model, Event, Effect>(
    private val loopName: String,
    private val update: (Model, Event) -> Next<Model, Effect>,
    private val effectHandler: (Effect) -> Flow<Event>,
    private val eventSource: Connectable<Model, Event>,
    private val startFrom: Model,
    // TODO: effect runner, event runner, logger, something like init/initial effects
) : Disposable {
    public var mostRecentModel: Model? = null

    private lateinit var lifecycleManager: LifecycleManager<Event>
    private lateinit var modelObservable: ObservableItems<Model>

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun connect() {
        // wire up loop:
        // - event Runner: ensure safe and atomic state mutation
        // - split: models to one loop, effects to another
        // - effect loop:
        //    - flatmap effect handler
        //    - join back
        // - model loop:
        //    - observer
        //    - event sources
        //    - ui (included in event source)
        // - life cycle handler

        // this should be passed in to support single-threaded execution?
        val scope = CoroutineScope(Dispatchers.Default)

        val eventRunner = SwitchToContext<Event> { count -> newSingleThreadRunner("Event Runner ($loopName:$count)") }
        // maybe as an alternative to the above ^, one could wrap a Connectable in an Actor. For that use case, it would
        // be nice to have some sort of 'one-to-one connectable', where each piece of input corresponds exactly to one piece of output. like a single, sorta.
        // alternatively/additionally, some type that defines a stateful connectable.
        // the StateHolder could use an Actor internally and provide guarantees. But that would be less flexible for the single-threaded use case

        // TODO: support lifecycle events like init, (etc.?), here. That would enable kicking off initial effects. It would be easy to do by wrapping the Event type inside a sealed class.
        val stateHolder: StateHolder<Event, Next<Model, Effect>> =
            StateHolder(wrap(update), Next(startFrom, emptySet()))
        val splitNexts = Split<Next<Model, Effect>>()
        val separateEffects = FlatMapFlow<Next<Model, Effect>, Effect>(scope) { it.effects.asFlow() }
        val effectHandlerConnectable = FlatMapFlow<Effect, Event>(scope) { effectHandler.invoke(it) }
        val separateModels = MapItems<Next<Model, Effect>, Model> { next -> next.model }
        modelObservable = ObservableItems()
        val joinEvents = Join<Event>()
        lifecycleManager = LifecycleManager(scope)

        val joinConnection = joinEvents.connect(lifecycleManager)

        val effectsConnection = separateEffects.connect(effectHandlerConnectable.connect(joinConnection))
        val modelsConnection = separateModels.connect(modelObservable.connect(eventSource.connect(joinConnection)))

        val splitNextsConnection = splitNexts.connect(effectsConnection)
        splitNextsConnection.connect(modelsConnection)

        eventRunner.connect(stateHolder.connect(splitNextsConnection))
    }

    // observe + dispatchEvent is really a Connectable, too.

    public fun dispatchEvent(event: Event): Unit = lifecycleManager.consume(event)

    override fun dispose() {
        TODO("Not yet implemented")
    }

    public fun observe(observer: Consumer<Model>): Disposable = modelObservable.observe(observer)

    private fun wrap(update: (Model, Event) -> Next<Model, Effect>): (Next<Model, Effect>, Event) -> Next<Model, Effect> =
        { next, event -> update(next.model, event) }
}