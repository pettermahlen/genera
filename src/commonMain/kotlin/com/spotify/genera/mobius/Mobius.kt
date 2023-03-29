package com.spotify.genera.mobius

public data class Next<Model, Effect>(
        public val model: Model,
        public val effects: Set<Effect>,
)