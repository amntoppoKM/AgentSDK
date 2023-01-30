package io.kommunicate.agent.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applozic.mobicomkit.api.MobiComKitClientService
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference
import com.applozic.mobicommons.ApplozicService
import com.applozic.mobicommons.commons.core.utils.Utils
import com.applozic.mobicommons.people.channel.Channel
import io.kommunicate.agent.R
import io.kommunicate.agent.model.KmResult
import io.kommunicate.agent.model.KmTag
import io.kommunicate.agent.model.Resource
import io.kommunicate.agent.model.SingleLiveEvent
import io.kommunicate.agent.repositories.KmTagsRepository
import kotlinx.coroutines.launch

class KmTagsViewModel : ViewModel() {

    private val tagsRepository: KmTagsRepository = KmTagsRepository(ApplozicService.getAppContext())

    private val createTagLiveData: SingleLiveEvent<Resource<KmTag>> = SingleLiveEvent()
    private val addOrRemoveTagLiveData: MutableLiveData<Resource<List<KmTag>>> = MutableLiveData()
    private val appliedTagIdsLiveData: MutableLiveData<List<Int>> = MutableLiveData()
    private val tagsLiveData: MutableLiveData<Resource<List<KmTag>>> = MutableLiveData()

    companion object TagsCache {
        private val tagList: MutableList<KmTag> = ArrayList()

        fun getTagListFromCache(appliedTagIds: List<Int>?): List<KmTag> {
            if (appliedTagIds.isNullOrEmpty()) {
                return tagList
            }
            return tagList.map { KmTag(it, appliedTagIds.contains(it.id)) }
        }

        fun addTagListToCache(newTagList: List<KmTag>) {
            if (tagList.isNotEmpty()) {
                tagList.clear()
            }
            tagList.addAll(newTagList)
        }

        fun addTag(tag: KmTag) {
            tagList.add(tag)
        }

        fun removeTag(tag: KmTag) {
            tagList.remove(tag)
        }

        fun clearTags() {
            tagList.clear()
        }
    }

    fun getTagList(appliedTagIds: List<Int>?): MutableLiveData<Resource<List<KmTag>>> {
        when (getTagListFromCache(appliedTagIds).isNullOrEmpty()) {
            false -> {
                tagsLiveData.postValue(Resource.success(getTagListFromCache(appliedTagIds)))
            }

            true -> {
                viewModelScope.launch {
                    val result = try {
                        tagsRepository.getTagListResponse()
                    } catch (e: Exception) {
                        KmResult.Error(e)
                    }

                    when (result) {
                        is KmResult.Success<List<KmTag>> -> {
                            addTagListToCache(result.data)
                            tagsLiveData.postValue(Resource.success(getTagListFromCache(appliedTagIds)))
                        }

                        else -> tagsLiveData.postValue(Resource.error("Unable to fetch tags", null))
                    }
                }
            }
        }
        return tagsLiveData
    }

    fun updateTagListLiveData(appliedTagIds: List<Int>?) {
        tagsLiveData.postValue(Resource.success(getTagListFromCache(appliedTagIds)))
    }


    fun createNewTag(tag: KmTag): MutableLiveData<Resource<KmTag>> {
        tag.applicationId = MobiComKitClientService.getApplicationKey(ApplozicService.getAppContext())
        tag.createdBy = MobiComUserPreference.getInstance(ApplozicService.getAppContext()).userId
        if (tag.color == null) {
            tag.color = Utils.getString(ApplozicService.getAppContext(), R.color.km_tag_background_color)
        }
        viewModelScope.launch {
            val result = try {
                tagsRepository.createTagWithResponse(tag)
            } catch (e: Exception) {
                e.printStackTrace()
                KmResult.Error(e)
            }
            when (result) {
                is KmResult.Success<KmTag> -> {
                    createTagLiveData.postValue(Resource.success(result.data))
                    addTag(result.data)
                }
                else -> createTagLiveData.postValue(Resource.error("Failed to create tag", null))
            }
        }
        return createTagLiveData
    }

    fun getAppliedTagsLiveData(): MutableLiveData<List<Int>> {
        return appliedTagIdsLiveData
    }

    fun updateAppliedTagsLiveData(appliedTagIds: List<Int>?) {
        appliedTagIdsLiveData.postValue(appliedTagIds)
    }

    fun getAddorRemoveTag(): MutableLiveData<Resource<List<KmTag>>> {
        return addOrRemoveTagLiveData
    }

    fun renameTag(tag: KmTag, newTag: KmTag, channel: Channel): MutableLiveData<Resource<String>> {
        val data: MutableLiveData<Resource<String>> = MutableLiveData()
        viewModelScope.launch {
            val result = try {
                tagsRepository.renameTagWithResponse(tag, newTag, channel)
            } catch (e: Exception) {
                e.printStackTrace()
                KmResult.Error(e)
            }
            when (result) {
                is KmResult.Success<String> -> {
                    data.postValue(Resource.success(result.data))
                }
                else ->  data.postValue(Resource.error("Failed to create tag", null))
            }
        }
        return data
    }

    fun deleteTag(tag: KmTag, channel: Channel): MutableLiveData<Resource<String>> {
        val data: MutableLiveData<Resource<String>> = MutableLiveData()
        viewModelScope.launch {
            val result = try {
                tagsRepository.deleteTagWithResponse(tag)
            } catch (e: Exception) {
                e.printStackTrace()
                KmResult.Error(e)
            }
            when (result) {
                is KmResult.Success<String> -> {
                    data.postValue(Resource.success(result.data))
                }
                else ->  data.postValue(Resource.error("Failed to create tag", null))
            }
        }
        return data
    }


    fun addOrRemoveTag(channel: Channel, tag: KmTag, add: Boolean): MutableLiveData<Resource<List<KmTag>>> {
        viewModelScope.launch {
            val result = try {
                tagsRepository.addOrRemoveTagToConversation(channel, listOf(tag), add)
            } catch (e: Exception) {
                e.printStackTrace()
                KmResult.Error(e)
            }

            when (result) {

                is KmResult.Success<List<KmTag>> -> {
                    addOrRemoveTagLiveData.postValue(Resource.success(result.data))
                }

                else -> addOrRemoveTagLiveData.postValue(Resource.error("Failed to " + if (add) "add" else "remove" + " tag", null))
            }
        }
        return addOrRemoveTagLiveData
    }
}