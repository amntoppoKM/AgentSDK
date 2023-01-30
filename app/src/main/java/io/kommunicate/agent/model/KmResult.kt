package io.kommunicate.agent.model

sealed class KmResult<out R> {
    data class Success<out T>(val data: T) : KmResult<T>()
    data class Error(val exception: Exception) : KmResult<Nothing>()
}
