package io.kommunicate.agent.conversations.repositories

import android.content.Context
import com.applozic.mobicomkit.api.MobiComKitClientService
import com.applozic.mobicommons.json.GsonUtils
import com.google.gson.reflect.TypeToken
import io.kommunicate.agent.model.*
import io.kommunicate.agent.services.AgentClientService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.kommunicate.models.KmAppSettingModel;

class KmAppSettingsRepository(context: Context) : AgentClientService(context) {
    object Urls {
        const val KM_GET_APP_SETTINGS = "/rest/ws/settings/application/"
    }

    private fun getAppSettingsUrl(applicationId: String): String {
        return kmBaseUrl + Urls.KM_GET_APP_SETTINGS + applicationId
    }

    suspend fun getAppSettings(): KmResult<KmAppSettingModel> {
        val applicationKey = MobiComKitClientService.getApplicationKey(context)

        return withContext(Dispatchers.IO) {

            val apiResponse: KmAppSettingModel = GsonUtils.getObjectFromJson(
                getResponse(
                    getAppSettingsUrl(applicationKey)
                ),
                object : TypeToken<KmAppSettingModel>() {}.type
            ) as KmAppSettingModel

            if (apiResponse.isSuccess) {
                KmResult.Success(apiResponse)
            } else {
                KmResult.Error(Exception("Network request for App Settings failed"))
            }
        }
    }
}