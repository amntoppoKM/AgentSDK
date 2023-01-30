package io.kommunicate.agent.asyncs

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import com.applozic.mobicomkit.api.attachment.FileClientService
import io.kommunicate.agent.conversations.viewmodels.KmConversationViewModel
import io.kommunicate.agent.services.AgentService
import io.kommunicate.callbacks.KmCallback
import java.io.File

class KmProfileImageUploadTask(var isSaveFile: Boolean, var fileUri: Uri?, var file: File?, var context: Context?, var callback: KmCallback) : AsyncTask<Void, Void, Boolean>() {
    var fileClientService: FileClientService? = null
    var agentService: AgentService? = null

    init {
        fileClientService = FileClientService(context)
        agentService = AgentService(context)
    }

    override fun doInBackground(vararg params: Void?): Boolean? {
        try {
            var response: String? = null
            if (fileUri != null) {
                if (isSaveFile) {
                    fileClientService!!.writeFile(fileUri, file)
                }
                response = fileClientService!!.uploadProfileImage(file!!.absolutePath)
            }
            val user = KmConversationViewModel.getUser(context)
            user.imageLink = response
            val apiResponse = agentService?.updateDisplayNameORImageLink(user)
            return apiResponse != null && apiResponse.isSuccess
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun onPostExecute(result: Boolean?) {
        if (result != null && result) {
            callback.onSuccess(result)
        } else {
            callback.onFailure(result)
        }
    }
}