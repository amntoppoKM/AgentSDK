package io.kommunicate.agent.adapters

import android.app.Activity
import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.applozic.mobicommons.ApplozicService
import io.kommunicate.agent.R
import io.kommunicate.agent.databinding.KmTagsItemLayoutBinding
import io.kommunicate.agent.listeners.KmTagsCallback
import io.kommunicate.agent.model.KmTag
import java.util.*
import kotlin.collections.ArrayList

class KmTagsListAdapter(private var tagList: MutableList<KmTag>, private val kmCallback: KmTagsCallback, private val context: Activity) : RecyclerView.Adapter<KmTagsListAdapter.KmTagsViewHolder>(), Filterable {

    class KmTagsViewHolder(val tagsItemLayoutBinding: KmTagsItemLayoutBinding) : RecyclerView.ViewHolder(tagsItemLayoutBinding.root)

    private var originalList: MutableList<KmTag>? = null
    private var exactMatchList: MutableList<KmTag> = ArrayList()
    private var searchText: String? = null
    private val highlightTextSpan = TextAppearanceSpan(ApplozicService.getAppContext(), R.style.KmAssigneeNameBold)
    private var newTag: KmTag? = null
    private var longClickedTag: KmTag? = null
    private var renameTag: KmTag? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KmTagsViewHolder {
        return KmTagsViewHolder(KmTagsItemLayoutBinding.inflate(LayoutInflater.from(parent.context)))
    }

    fun setTagList(tagList: List<KmTag>) {
        this.tagList.clear()
        this.tagList.addAll(tagList)
        this.tagList.sortBy { it.name }
        notifyDataSetChanged()
    }

    fun addTag(tag: KmTag) {
        if (newTag != null) {
            tagList.remove(newTag)
            newTag = null
        }
        this.tagList.add(tag)
        this.originalList?.add(tag)
        notifyDataSetChanged()
    }

    fun renameTag(oldtag: KmTag, newTag: KmTag) {
        this.tagList.remove(oldtag)
        this.tagList.add(newTag)
        this.originalList?.remove(oldtag)
        this.originalList?.add(newTag)
        tagList.sortBy { it.name }
        resetAdapter()
    }

    fun deleteTag(tag: KmTag) {
        this.tagList.remove(tag)
        this.originalList?.remove(tag)
        tagList.sortBy { it.name }
        resetAdapter()
    }

    fun onTagLongClicked(tag: KmTag) {
        this.longClickedTag = tag
        this.renameTag = null
        notifyDataSetChanged()
    }

    fun onTagRenameIconClick(tag: KmTag) {
        this.renameTag = tag
        notifyDataSetChanged()
    }

    fun resetAdapter() {
        this.longClickedTag = null
        this.renameTag = null
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return tagList.size
    }

    override fun onBindViewHolder(holder: KmTagsViewHolder, position: Int) {
        val tag = tagList[position]
        holder.tagsItemLayoutBinding.kmTag = tag

        val startIndex: Int = indexOfSearchQuery(tag.name)
        if (startIndex != -1) {
            val highlightedName = SpannableString(tag.name)
            highlightedName.setSpan(highlightTextSpan, startIndex, startIndex + searchText!!.length, 0)
            holder.tagsItemLayoutBinding.kmNameTextView.text = highlightedName
        }
        holder.tagsItemLayoutBinding.isLongClicked = tag == longClickedTag
        holder.tagsItemLayoutBinding.isRenameClicked = tag == renameTag
        if(tag == renameTag) {
            holder.tagsItemLayoutBinding.kmNameEditText.requestFocus()
            holder.tagsItemLayoutBinding.kmNameEditText.setSelection(holder.tagsItemLayoutBinding.kmNameEditText.length())

       }
        holder.tagsItemLayoutBinding.kmCallback = this.kmCallback
        holder.tagsItemLayoutBinding.executePendingBindings()
    }

    override fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val oReturn = FilterResults()
                val results: MutableList<KmTag> = ArrayList()

                if (originalList == null) {
                    originalList = tagList
                }

                if (!constraint.isNullOrEmpty()) {
                    searchText = constraint.toString()
                    if (originalList != null && originalList!!.isNotEmpty()) {
                        for (tag in originalList!!) {
                            if (tag.name.toLowerCase().contains(constraint.toString().toLowerCase())) {
                                results.add(tag)
                            }

                            if (tag.name.equals(constraint.toString(), true)) {
                                exactMatchList.add(tag)
                            }
                        }
                    }
                    oReturn.values = results
                } else {
                    oReturn.values = originalList
                }
                return oReturn
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                tagList = results.values as MutableList<KmTag>

                when (exactMatchList.isEmpty()) {
                    false -> exactMatchList.clear()

                    true -> {
                        val kmTag = searchText?.let { KmTag(it) }
                        kmTag?.newTag = true
                        if (kmTag != null) {
                            tagList.remove(newTag)
                            newTag = kmTag
                            if (!constraint.isNullOrEmpty()) {
                                tagList.add(kmTag)
                            }
                        }
                    }
                }
                resetAdapter()
                notifyDataSetChanged()
            }
        }
    }

    private fun indexOfSearchQuery(name: String): Int {
        return if (!searchText.isNullOrEmpty()) {
            name.toLowerCase(Locale.getDefault()).indexOf(searchText!!.toLowerCase(Locale.getDefault()))
        } else -1
    }
}