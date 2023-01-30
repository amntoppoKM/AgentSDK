package io.kommunicate.agent.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.applozic.mobicomkit.api.MobiComKitClientService
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference
import com.applozic.mobicomkit.channel.service.ChannelService
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast
import com.applozic.mobicommons.ApplozicService
import com.applozic.mobicommons.json.GsonUtils
import com.applozic.mobicommons.people.channel.Channel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.reflect.TypeToken
import io.kommunicate.agent.AgentSharedPreference
import io.kommunicate.agent.KmAgentRegistrationResponse
import io.kommunicate.agent.R
import io.kommunicate.agent.adapters.KmTagsListAdapter
import io.kommunicate.agent.asyncs.AgentGetStatusTask
import io.kommunicate.agent.conversations.adapters.KmMoreItemsListAdapter
import io.kommunicate.agent.databinding.KmTagsLayoutBinding
import io.kommunicate.agent.listeners.KmTagsCallback
import io.kommunicate.agent.model.KmTag
import io.kommunicate.agent.model.Status
import io.kommunicate.agent.viewmodels.KmTagsViewModel
import io.kommunicate.callbacks.KmCallback
import kotlin.collections.ArrayList

class KmTagsFragment(private var kmMoreItemsListAdapter: KmMoreItemsListAdapter) : BottomSheetDialogFragment(), SearchView.OnQueryTextListener, KmTagsCallback {

    lateinit var binding: KmTagsLayoutBinding
    private val tagsViewModel by activityViewModels<KmTagsViewModel>()
    lateinit var tagsListAdapter: KmTagsListAdapter
    private var appliedTagList: MutableList<Int>? = null
    private lateinit var channel: Channel

    companion object {
        const val FRAG_TAG = "KmTagsFragment"
        const val APPLIED_TAG_IDS = "appliedTagIds"
        const val CONVERSATION_JSON = "conversationJson"

        //Tags are currently available for all Plans, so this method always returns true.
        //If Tags feature become available to certain Pricing Plan, then edit this method to return if it's available with the customer's plan.
        fun isTagsFeatureAvailable(context: Context): Boolean {
            return true
            //return MobiComUserPreference.getInstance(context).pricingPackage >= KmAgentRegistrationResponse.PricingPackage.KOMMUNICATE_GROWTH_MONTHLY.value
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appliedTagList = GsonUtils.getObjectFromJson(arguments?.getString(APPLIED_TAG_IDS), object : TypeToken<MutableList<Int>>() {}.type) as MutableList<Int>
        channel = GsonUtils.getObjectFromJson(arguments?.getString(CONVERSATION_JSON), object : TypeToken<Channel>() {}.type) as Channel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.km_tags_layout, container, false)
        binding.kmAssigneeSearchView.setOnQueryTextListener(this)
        setupTagList()

        binding.kmDoneButton.setOnClickListener {
            dismissAllowingStateLoss()
        }

        KmToast.makeText(context, getString(R.string.km_tags_info), R.color.km_blue_toast_color, 0, R.color.white, 0, Toast.LENGTH_SHORT).show();

        return binding.root
    }


    private fun setupTagList() {
        tagsListAdapter = activity?.let { KmTagsListAdapter(ArrayList(), this, it) }!!
        binding.kmTagsRecyclerView.adapter = tagsListAdapter

        tagsViewModel.getTagList(appliedTagList).observe(viewLifecycleOwner, {
            when (it.status) {
                Status.SUCCESS -> if (it.data != null) {
                    tagsListAdapter.setTagList(it.data)
                    kmMoreItemsListAdapter.updateKmTagList(it.data.filter { it.applied })
                    appliedTagList = it.data.filter { it.applied }.map { it.id } as MutableList<Int>
                }

                Status.ERROR -> KmToast.error(context, it.message, Toast.LENGTH_SHORT).show()

                Status.LOADING -> {
                }
            }
        })

        tagsViewModel.getAddorRemoveTag().observe(this, {
                itMain ->
            val receivedList: List<KmTag>? = itMain.data
            appliedTagList = receivedList?.map { it.id } as MutableList<Int>
            tagsViewModel.getTagList(appliedTagList)
            tagsViewModel.updateAppliedTagsLiveData(appliedTagList)

        })
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        tagsListAdapter.filter?.filter(newText)
        return false
    }


    override fun onSingleClick(kmTag: KmTag) {
        tagsListAdapter.resetAdapter()
        if (kmTag.newTag) {    //Create new tag request
            tagsViewModel.createNewTag(kmTag).observe(this, {
                it.data?.let { it1 -> tagsListAdapter.addTag(it1) }
                KmToast.success(context, R.string.km_tag_created, Toast.LENGTH_SHORT).show()
            })
            return
        }

        //If the applied tag list size is 5 and a new request to apply a tag comes, reject it
        if (!kmTag.applied && appliedTagList?.size!! >= 5) {
            KmToast.error(context, R.string.km_tag_exceeded_error, Toast.LENGTH_SHORT).show()
            return
        }

        //If the tag is applied, remove it, else add it
        tagsViewModel.addOrRemoveTag(channel, kmTag, !kmTag.applied)
        KmToast.success(context,  if(!kmTag.applied) R.string.km_tag_applied else R.string.km_tag_removed, Toast.LENGTH_SHORT).show()


    }

    override fun onLongClick(kmTag: KmTag): Boolean {
        if(kmTag.applicationId != MobiComKitClientService.getApplicationKey(context)) {
            return true
        }
            tagsListAdapter.onTagLongClicked(kmTag)
            return true
    }

    override fun onDeleteTag(kmTag: KmTag) {
        tagsViewModel.deleteTag(kmTag, channel).observe(this, {
            it.data?.let { it1 -> KmToast.error(context, it1, Toast.LENGTH_SHORT).show()
            }
            if(it.isSuccess()) {
                tagsListAdapter.deleteTag(kmTag)
                kmMoreItemsListAdapter.updateKmTagList(KmTagsViewModel.getTagListFromCache(appliedTagList).filter { it.applied })
                updateChannelWithNewTags(channel, KmTagsViewModel.getTagListFromCache(appliedTagList).filter { it.applied })
                KmTagsViewModel.clearTags()
                if(appliedTagList?.contains(kmTag.id) == true) {
                    appliedTagList?.remove(kmTag.id)
                }
            }
        })
    }

    override fun onRenameTag(kmTag: KmTag) {
        tagsListAdapter.onTagRenameIconClick(kmTag)

    }

    override fun onRenameSuccess(kmTag: KmTag, newTagName: String) {
        val newTag = KmTag(kmTag, newTagName)
        tagsViewModel.renameTag(kmTag, newTag, channel).observe(this, {
            if(it.isSuccess()) {
                if (it.data?.equals(getString(R.string.km_tag_updated_successfully)) == true) {
                    it.data.let { it1 ->
                        KmToast.success(context, it1, Toast.LENGTH_SHORT).show()
                    }
                    tagsListAdapter.renameTag(kmTag, newTag)
                    kmMoreItemsListAdapter.updateKmTagList(
                        KmTagsViewModel.getTagListFromCache(
                            appliedTagList
                        ).filter { it.applied })
                    updateChannelWithNewTags(
                        channel,
                        KmTagsViewModel.getTagListFromCache(appliedTagList).filter { it.applied })
                    KmTagsViewModel.clearTags()
                } else {
                    it.data?.let { it1 ->
                        KmToast.error(
                            context,
                            getString(R.string.km_tag_already_exists, newTagName),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
//            if(it.isSuccess()) {
//                tagsListAdapter.renameTag(kmTag, newTag)
//                kmMoreItemsListAdapter.updateKmTagList(KmTagsViewModel.getTagListFromCache(appliedTagList).filter { it.applied })
//                updateChannelWithNewTags(channel, KmTagsViewModel.getTagListFromCache(appliedTagList).filter { it.applied })
//                KmTagsViewModel.clearTags()
//            }
        })
    }

    override fun onCancel() {
        tagsListAdapter.resetAdapter()
    }

    private fun updateChannelWithNewTags(channel: Channel, newTagList: List<KmTag>?) {
        if (channel.metadata != null) {
            channel.metadata[KmTag.KM_TAGS] = GsonUtils.getJsonFromObject(newTagList, object : TypeToken<List<KmTag>>() {}.type)
            ChannelService.getInstance(context).updateChannel(channel)
        }
    }

}