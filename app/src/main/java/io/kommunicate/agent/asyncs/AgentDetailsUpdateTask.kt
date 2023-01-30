package io.kommunicate.agent.asyncs

import android.content.Context
import com.applozic.mobicomkit.api.account.user.AlUserUpdateTask
import com.applozic.mobicomkit.api.account.user.User
import com.applozic.mobicomkit.feed.ApiResponse
import com.applozic.mobicomkit.listners.AlCallback
import io.kommunicate.agent.services.AgentService

class AgentDetailsUpdateTask(val context: Context?, val user: User?, val callback: AlCallback?) : AlUserUpdateTask(context, user, callback) {

    override fun doInBackground(): ApiResponse<*> {
        return AgentService(context).updateDisplayNameORImageLink(user)
    }
}