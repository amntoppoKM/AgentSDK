package io.kommunicate.agent.permissions

interface KmPermissionListener {
    fun onAction(code: Int, didGrant: Boolean)
}