package io.kommunicate.agent.conversations.adapters

import android.content.Context
import android.util.Pair
import com.applozic.mobicomkit.api.account.user.User
import com.applozic.mobicomkit.api.conversation.Message
import com.applozic.mobicomkit.api.conversation.MessageIntentService
import com.applozic.mobicomkit.uiwidgets.conversation.adapter.DetailedConversationAdapter
import com.applozic.mobicommons.ApplozicService
import com.applozic.mobicommons.commons.core.utils.Utils
import com.applozic.mobicommons.emoticon.EmojiconHandler
import com.applozic.mobicommons.people.channel.Channel
import com.applozic.mobicommons.people.contact.Contact
import io.kommunicate.agent.R

class KmMessageListAdapter(
    context: Context,
    textViewResourceId: Int,
    messageList: List<Message>,
    contact: Contact?,
    channel: Channel?,
    messageIntentClass: Class<MessageIntentService>,
    emojiconHandler: EmojiconHandler?
) :
    DetailedConversationAdapter(
        context,
        textViewResourceId,
        messageList,
        contact,
        channel,
        messageIntentClass,
        emojiconHandler
    ) {
    override fun getReceivedMessageBgColors(contact: Contact?, message: Message?): Pair<Int, Int> {
        if (!isHtmlTypeMessage(message) && contact != null && contact.roleType != User.RoleType.USER_ROLE.value) {
            val colorId = Utils.getColor(
                ApplozicService.getAppContext(),
                R.color.km_other_agents_message_background_color
            )
            return Pair(colorId, colorId)
        }
        return super.getReceivedMessageBgColors(contact, message)
    }
}