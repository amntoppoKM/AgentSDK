package io.kommunicate.agent.repositories

import KmNetworkResponse
import android.content.Context
import com.applozic.mobicommons.json.GsonUtils
import com.google.gson.reflect.TypeToken
import io.kommunicate.agent.model.KmResult
import io.kommunicate.agent.model.KmSAMLResponse
import io.kommunicate.agent.services.AgentClientService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KmAuthRepository(context: Context) : AgentClientService(context) {

    companion object {
        const val EMAIL_REGEX =
            "^[\\w!#\$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#\$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}\$"
    }

    object Urls {
        const val SAML_INIT_URL = "/rest/ws/login/saml/init"
    }

    private fun getSamlInitUrl(): String {
        return kmBaseUrl + Urls.SAML_INIT_URL
    }

    suspend fun initSamlLogin(email: String, applicationId: String?): KmResult<KmSAMLResponse> {
        val dataMap = HashMap<String, String>()
        dataMap["email"] = email
        if (!applicationId.isNullOrEmpty()) {
            dataMap["applicationId"] = applicationId
        }

        return withContext(Dispatchers.IO) {
            val response = postData(
                getSamlInitUrl(),
                GsonUtils.getJsonFromObject(dataMap, Map::class.java),
                true
            )
            val apiResponse: KmNetworkResponse<KmSAMLResponse> = GsonUtils.getObjectFromJson(
                response,
                object : TypeToken<KmNetworkResponse<KmSAMLResponse>>() {}.type
            ) as KmNetworkResponse<KmSAMLResponse>

            when (apiResponse.isSuccess()) {
                true -> KmResult.Success(apiResponse.data)
                false -> KmResult.Error(Exception("Network request for SAML init failed"))
            }
        }
    }
}