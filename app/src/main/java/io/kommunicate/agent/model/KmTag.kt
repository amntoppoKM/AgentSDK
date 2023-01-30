package io.kommunicate.agent.model

import android.widget.TextView
import androidx.annotation.Keep
import androidx.databinding.BindingAdapter
import com.applozic.mobicommons.json.GsonUtils
import com.applozic.mobicommons.json.JsonMarker
import com.applozic.mobicommons.people.channel.Channel
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

@Keep
data class KmTag(@SerializedName("name") @Expose var name: String,
                 @SerializedName("id") @Expose val id: Int = 0,
                 @SerializedName("color")  var color: String? = null,
                 @SerializedName("applicationId") @Expose var applicationId: String?,
                 @SerializedName("createdBy") var createdBy: String?,
                 var newTag: Boolean = false,
                 var applied: Boolean = false) : JsonMarker() {

    constructor(tag: KmTag, applied: Boolean) : this(tag.name, tag.id, tag.color, tag.applicationId, tag.createdBy, tag.newTag, applied)
    constructor(name: String) : this(name, 0, null, null, null, false, false)
    constructor(name: String, id: Int, applicationId: String?) : this(name, id, null, applicationId, null, false, false)
    constructor(tag: KmTag, newName: String) : this(newName, tag.id, tag.color, tag.applicationId, tag.createdBy, tag.newTag, tag.applied)

    companion object {
        const val KM_TAGS = "KM_TAGS"

        //returns the filtered list of tag from the super set tag list
        //based on the tags applied to the channel, filtered by tagId
        fun getFilteredTagList(channel: Channel, allTagList: List<KmTag>): List<KmTag>? {
            if (channel.metadata != null && channel.metadata.containsKey(KM_TAGS)) {
                val channelTagList: List<KmTag> = GsonUtils.getObjectFromJson(channel.metadata[KM_TAGS], object : TypeToken<List<KmTag>>() {}.type) as List<KmTag>
                return allTagList.filter { allTag -> channelTagList.any { it.id == allTag.id } }
            }
            return null
        }

        //return the list of tag IDs applied to this conversation
        fun getAppliedTagIds(channel: Channel): List<Int>? {
            if (channel.metadata != null && channel.metadata.containsKey(KM_TAGS)) {
                val channelTagList: List<KmTag> = GsonUtils.getObjectFromJson(channel.metadata[KM_TAGS], object : TypeToken<List<KmTag>>() {}.type) as List<KmTag>
                return channelTagList.map { it.id }
            }
            return listOf()
        }
    }
}

@BindingAdapter("tagName")
fun setTagName(textView: TextView, tag: KmTag) {
    textView.text = if (tag.newTag) "Create" else tag.name
}