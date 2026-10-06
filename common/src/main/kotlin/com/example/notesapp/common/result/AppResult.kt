package com.example.notesapp.common.result

sealed interface AppResult<out D, out E> {
    data class Success<out D>(val data: D) : AppResult<D, Nothing>
    data class Error<out E>(val error: E) : AppResult<Nothing, E>
}

inline fun <D, E, R> AppResult<D, E>.map(transform: (D) -> R): AppResult<R, E> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}
