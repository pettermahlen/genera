@file:OptIn(ExperimentalCoroutinesApi::class)

package com.spotify.genera.mobius

import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

@OptIn(DelicateCoroutinesApi::class)
public actual fun newSingleThreadRunner(name: String): CloseableCoroutineDispatcher = newSingleThreadContext(name)