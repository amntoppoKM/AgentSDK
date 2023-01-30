package io.kommunicate.agent.repositories

import android.content.Context
import com.applozic.mobicomkit.feed.ApiResponse
import com.applozic.mobicommons.json.GsonUtils
import com.google.gson.reflect.TypeToken
import io.kommunicate.KmException
import io.kommunicate.agent.model.KmResult
import io.kommunicate.agent.model.KmTeam
import io.kommunicate.agent.services.AgentClientService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KmTeamRepository(context: Context) : AgentClientService(context) {
    object Urls {
        const val KM_GET_TEAM_LIST_URL = "/rest/ws/team/list"
        const val KM_UPDATE_TEAM_URL = "/rest/ws/group/update"
        const val KM_GET_TEAM_DATA = "/rest/ws/team/get?clientGroupId="
        const val DEFAULT_TEAM_NAME = "km_default_team"
    }

    private fun getTeamsUrl(): String {
        return baseUrl + Urls.KM_GET_TEAM_LIST_URL
    }

    private fun getTeamDataUrl(teamName: String): String {
        return baseUrl + Urls.KM_GET_TEAM_DATA + teamName
    }

    suspend fun getTeamListResponse(): KmResult<List<KmTeam>> {
        return withContext(Dispatchers.IO) {
            val apiResponse = GsonUtils.getObjectFromJson(getAlResponse(getTeamsUrl()),
                    object : TypeToken<ApiResponse<List<KmTeam>>>() {}.type) as ApiResponse<List<KmTeam>>

            when (apiResponse.isSuccess) {
                false -> KmResult.Error(KmException("Unable to fetch teams"))
                true -> KmResult.Success(apiResponse.response)
            }
        }
    }

    suspend fun getTeamDataResponse(teamName: String): KmResult<KmTeam> {
        return withContext(Dispatchers.IO) {
            val apiResponse = GsonUtils.getObjectFromJson(getAlResponse(getTeamDataUrl(teamName)),
                    object : TypeToken<ApiResponse<KmTeam>>() {}.type) as ApiResponse<KmTeam>

            when (apiResponse.isSuccess) {
                false -> KmResult.Error(KmException("Unable to fetch teams"))
                true -> KmResult.Success(apiResponse.response)
            }
        }
    }
}