package io.kommunicate.agent.cache

import com.applozic.mobicomkit.api.conversation.Message

object KmConversationCache {

    private var conversationCache = mutableMapOf<Int, List<Message>>()

    fun getConversationByKey(key: Int): List<Message>? {
        return conversationCache[key]
    }

    fun setConversationListByKey(key: Int, conversationList: List<Message>) {
        conversationCache[key] = conversationList
    }
}