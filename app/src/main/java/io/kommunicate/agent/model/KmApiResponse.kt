package io.kommunicate.agent.model

import androidx.annotation.Keep
import com.applozic.mobicommons.json.JsonMarker
import com.google.gson.annotations.SerializedName

@Keep
data class KmApiResponse<T>(@SerializedName("code") val code: String, @SerializedName("response") val response: T) : JsonMarker() {
    fun isSuccess(): Boolean {
        return AgentAPIResponse.SUCCESS == code
    }
}
