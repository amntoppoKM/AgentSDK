package io.kommunicate.agent.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.applozic.mobicomkit.api.conversation.Message
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast
import com.applozic.mobicommons.commons.core.utils.Utils
import io.kommunicate.agent.KmSearchView
import io.kommunicate.agent.R
import io.kommunicate.agent.conversations.adapters.KmSearchListAdapter
import io.kommunicate.agent.conversations.repositories.KmConversationListRepo


class SearchActivity : AppCompatActivity(), KmSearchView.KmSearchInterface {

    private lateinit var messageSearchRecyclerView: RecyclerView
    private lateinit var kmConversationSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var messageSearchInstructionTv: TextView
    private lateinit var searchListAdapter: KmSearchListAdapter
    private lateinit var kmSearchView: KmSearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        messageSearchRecyclerView = findViewById(R.id.km_message_search_recycler_view)
        kmConversationSwipeRefreshLayout = findViewById(R.id.km_conversation_swipe_refresh)
        messageSearchInstructionTv = findViewById(R.id.kmSearchInstructionTv)

        kmConversationSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light)

        val searchLayoutManager = LinearLayoutManager(this)
        searchLayoutManager.orientation = RecyclerView.VERTICAL
        messageSearchRecyclerView.layoutManager = searchLayoutManager

        searchListAdapter = KmSearchListAdapter(this)
        kmSearchView = KmSearchView(this, this)
    }

    private fun prepareMessageSearchList(messageList: List<Message?>?, searchText: String?) {
        if (messageList == null) {
            return
        }
        kmConversationSwipeRefreshLayout.isRefreshing = true

        if (messageList.isEmpty()) {
            setSearchInstructionText(Utils.getString(this, R.string.km_empty_message_search_text))
            showSearchInstruction(true)
        } else {
            showSearchInstruction(false)

            searchListAdapter.setMessageList(messageList)
            searchListAdapter.setSearchString(searchText)
            messageSearchRecyclerView.adapter = searchListAdapter
        }
    }

    private fun setSearchInstructionText(text: String?) {
        messageSearchInstructionTv.text = text
    }

    private fun showSearchInstruction(showInstruction: Boolean) {
        kmConversationSwipeRefreshLayout.isRefreshing = false

        messageSearchRecyclerView.visibility = if (showInstruction) View.GONE else View.VISIBLE
        messageSearchInstructionTv.visibility = if (showInstruction) View.VISIBLE else View.GONE
    }

    private fun processSearch(searchText: String) {
        kmConversationSwipeRefreshLayout.isRefreshing = true
        KmConversationListRepo.getMessageSearchListAsync(this, searchText) { messageList, exception ->
            if (exception != null) {
                KmToast.error(this, exception.localizedMessage, Toast.LENGTH_SHORT).show()
            } else {
                prepareMessageSearchList(messageList, searchText)
            }
        }
    }

    override fun onSearchTextCleared() {
        messageSearchRecyclerView.visibility = View.GONE
        messageSearchInstructionTv.visibility = View.GONE
    }

    override fun onSearchBackPressed() {
        onBackPressed()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onSearchSubmit(text: String?) {
        if (text.isNullOrEmpty()) {
            KmToast.error(this, Utils.getString(this, R.string.km_enter_search_text_error), Toast.LENGTH_SHORT).show()
        } else {
            processSearch(text)
        }
    }

    override fun onSearchTextChange(text: String?) {
        kmSearchView.toggleClearButtonVisibility(text.isNullOrBlank());
        if (!text.isNullOrEmpty()) {
            showSearchInstruction(true)
            setSearchInstructionText(applicationContext.getString(R.string.km_search_instructions))
        } else {
            onSearchTextCleared()
        }
    }
}