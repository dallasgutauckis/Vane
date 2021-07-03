package com.dallasgutauckis.vane.common.model

sealed class Async<out T> {
    object Loading : Async<Nothing>()
    data class Loaded<T>(val data: T) : Async<T>()
}

fun <T, F> Async<T>.fold(loading: () -> F, loaded: (data: T) -> F): F {
    return when (this) {
        is Async.Loaded -> loaded(this.data)
        Async.Loading -> loading()
    }
}
