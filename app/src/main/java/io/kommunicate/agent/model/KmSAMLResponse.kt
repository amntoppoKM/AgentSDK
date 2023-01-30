package io.kommunicate.agent.model

import androidx.annotation.Keep

@Keep
data class KmSAMLResponse(
    val message: String,
    val redirect: Boolean,
    val redirectionUrl: String,
    val applicationList: Any
)