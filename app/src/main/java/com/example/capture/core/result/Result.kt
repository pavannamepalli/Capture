package com.example.capture.core.result

import com.example.capture.core.error.AppError

sealed class Result<out T> {

    data class Success<out T>(val data: T) : Result<T>()

    data class Error(val error: AppError) : Result<Nothing>()

    object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success

    val isError: Boolean get() = this is Error

    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }


    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (AppError) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(error)
        is Loading -> onError(AppError.GenericError.UnknownError)
    }
}

fun <T> T.toSuccess(): Result<T> = Result.Success(this)

fun <T> AppError.toError(): Result<T> = Result.Error(this)

fun <T> Result<T>.toLoading(): Result<T> = Result.Loading

suspend fun <T> Result<T>.onSuccess(action: suspend (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

suspend fun <T> Result<T>.onError(action: suspend (AppError) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(error)
    }
    return this
}

suspend fun <T> Result<T>.onLoading(action: suspend () -> Unit): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}
