package io.kommunicate.agent.repositories

import android.content.Context
import com.applozic.mobicomkit.channel.service.ChannelService
import com.applozic.mobicomkit.feed.ApiResponse
import com.applozic.mobicommons.json.GsonUtils
import com.applozic.mobicommons.people.channel.Channel
import com.google.gson.reflect.TypeToken
import io.kommunicate.agent.model.*
import io.kommunicate.agent.services.AgentClientService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class KmTagsRepository(context: Context) : AgentClientService(context) {

    object Urls {
        const val KM_TAGS_URL = "/rest/ws/tags"
        const val KM_ADD_TAG_URL = "/rest/ws/group/add/tags"
        const val KM_REMOVE_TAG_URL = "/rest/ws/group/remove/tags"
    }

    private fun getTagsUrl(): String {
        return kmBaseUrl + Urls.KM_TAGS_URL
    }

    private fun getAddTagUrl(conversationId: Int): String {
        return baseUrl + Urls.KM_ADD_TAG_URL + "?groupId=" + conversationId
    }

    private fun getRemoveTagUrl(conversationId: Int): String {
        return baseUrl + Urls.KM_REMOVE_TAG_URL + "?groupId=" + conversationId
    }

    suspend fun getTagListResponse(): KmResult<List<KmTag>> {
        return withContext(Dispatchers.IO) {
            val result: AgentAPIResponse<KmTag> = GsonUtils.getObjectFromJson(getResponse(getTagsUrl()),
                    object : TypeToken<AgentAPIResponse<KmTag>>() {}.type) as AgentAPIResponse<KmTag>
            when (result.isSuccess) {
                true -> KmResult.Success(result.response)
                false -> KmResult.Error(Exception("Network request for tags failed"))
            }
        }
    }

    suspend fun renameTagWithResponse(tag: KmTag, newTag: KmTag, channel: Channel): KmResult<String> {
        return withContext(Dispatchers.IO) {
            val result = makePatchRequest(getTagsUrl(), GsonUtils.getJsonWithExposeFromObject(newTag, object : TypeToken<KmTag>() {}.type), true)
            val apiResponse = GsonUtils.getObjectFromJson(result,
                object : TypeToken<KmApiResponse<String>>() {}.type) as KmApiResponse<String>

            when (apiResponse.isSuccess()) {
                false -> {
                    KmResult.Error(Exception("Failed to rename tag"))
                }
                true -> {
                    KmResult.Success(apiResponse.response)
                }
            }
        }
    }

    suspend fun deleteTagWithResponse(tag: KmTag): KmResult<String> {
        return withContext(Dispatchers.IO) {
            val jsonObj = JSONObject()
            jsonObj.put("id", tag.id)
            val result = makeDeleteRequest(getTagsUrl(),jsonObj.toString() , true)
            val apiResponse = GsonUtils.getObjectFromJson(result,
                object : TypeToken<KmApiResponse<String>>() {}.type) as KmApiResponse<String>

            when (apiResponse.isSuccess()) {
                false -> {
                    KmResult.Error(Exception("Failed to delete tag"))
                }
                true -> {
                    KmResult.Success(apiResponse.response)
                }
            }
        }
    }

    suspend fun createTagWithResponse(tag: KmTag): KmResult<KmTag> {
        return withContext(Dispatchers.IO) {
            val json = JSONObject(GsonUtils.getJsonFromObject(tag,
                object : TypeToken<KmTag>() {}.type))
            json.remove("newTag")
            json.remove("applied")
            json.remove("id")
            val result = postData(getTagsUrl(), json.toString(), true)
            val apiResponse: KmApiResponse<KmTag> = GsonUtils.getObjectFromJson(result,
                    object : TypeToken<KmApiResponse<KmTag>>() {}.type) as KmApiResponse<KmTag>

            when (AgentAPIResponse.SUCCESS == apiResponse.code) {
                false -> KmResult.Error(Exception("Failed to create tag. Network request failed"))
                true -> {
                    KmResult.Success(apiResponse.response)
                }
            }
        }
    }

    suspend fun addOrRemoveTagToConversation(channel: Channel, tagList: List<KmTag>, add: Boolean): KmResult<List<KmTag>> {
        return withContext(Dispatchers.IO) {
            val result = makePatchRequest(if (add) getAddTagUrl(channel.key) else getRemoveTagUrl(channel.key), GsonUtils.getJsonFromObject(tagList,
                    object : TypeToken<List<KmTag>>() {}.type), false)
            val apiResponse = GsonUtils.getObjectFromJson(result,
                    object : TypeToken<ApiResponse<String>>() {}.type) as ApiResponse<String>

            when (apiResponse.isSuccess) {
                false -> KmResult.Error(Exception("Failed to + " + if (add) "add" else "remove" + " tag . Network request failed "))
                true -> {
                    val updatedTagList = GsonUtils.getObjectFromJson(apiResponse.response, object : TypeToken<List<KmTag>>() {}.type) as List<KmTag>
                    updateChannelWithNewTags(channel, updatedTagList)
                    KmResult.Success(updatedTagList)
                }
            }
        }
    }

    private fun updateChannelWithNewTags(channel: Channel, newTagList: List<KmTag>) {
        if (channel.metadata != null) {
            channel.metadata[KmTag.KM_TAGS] = GsonUtils.getJsonFromObject(newTagList, object : TypeToken<List<KmTag>>() {}.type)
            ChannelService.getInstance(context).updateChannel(channel)
        }
    }
}