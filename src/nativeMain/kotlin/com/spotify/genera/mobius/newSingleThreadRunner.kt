package com.spotify.genera.mobius

import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

@OptIn(ExperimentalCoroutinesApi::class)
public actual fun newSingleThreadRunner(name: String): CloseableCoroutineDispatcher = newSingleThreadContext(name)
