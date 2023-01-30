package io.kommunicate.agent.model

import android.text.TextUtils

data class KmTeam(
        val teamId: Int,
        val teamName: String,
        val teamMembers: List<String>,
        val admin: String,
        val agentRouting: Boolean
) : KmAssignee {
    override fun getTitle(): String {
        return if (TextUtils.isEmpty(this.teamName)) this.teamId.toString() else this.teamName
    }

    override fun getId(): String {
        return this.teamId.toString()
    }
}
