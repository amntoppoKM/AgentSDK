package io.kommunicate.agent.services

import android.content.Context
import android.text.TextUtils
import com.applozic.mobicomkit.api.HttpRequestUtils
import com.applozic.mobicomkit.api.MobiComKitClientService
import com.applozic.mobicomkit.api.account.register.RegisterUserClientService
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference
import com.applozic.mobicomkit.api.account.user.User
import com.applozic.mobicomkit.api.authentication.AlAuthService
import io.kommunicate.agent.exception.KmExceptionAnalytics
import java.net.HttpURLConnection

class KmHttpRequestUtils(val context: Context?) : HttpRequestUtils(context) {

    private val OF_USER_ID_HEADER = "Of-User-Id"
    private val X_AUTHORIZATION_HEADER = "x-authorization"
    private val APZ_APP_ID_HEADER = "Apz-AppId"
    private val APZ_PRODUCT_APP_HEADER = "Apz-Product-App"

    override fun addGlobalHeaders(connection: HttpURLConnection?, userId: String?) {
        try {
            if (MobiComKitClientService.getAppModuleName(context) != null) {
                connection!!.setRequestProperty(APP_MODULE_NAME_KEY_HEADER, MobiComKitClientService.getAppModuleName(context))
            }
            if (!TextUtils.isEmpty(userId)) {
                connection!!.setRequestProperty(OF_USER_ID_HEADER, userId)
            }
            val applicationKey = MobiComKitClientService.getApplicationKey(context)
            val userPreferences = MobiComUserPreference.getInstance(context)
            if (User.RoleType.AGENT.value == userPreferences.userRoleType && !TextUtils.isEmpty(userId)) {
                connection!!.setRequestProperty(APZ_APP_ID_HEADER, applicationKey)
                connection.setRequestProperty(APZ_PRODUCT_APP_HEADER, "true")
            } else {
                connection!!.setRequestProperty(APPLICATION_KEY_HEADER, applicationKey)
            }
            if (!AlAuthService.isTokenValid(context) && !isRefreshTokenInProgress) {
                RegisterUserClientService(context).refreshAuthToken(applicationKey, userPreferences.userId)
            }
            val userAuthToken = userPreferences.userAuthToken
            if (userPreferences.isRegistered && !TextUtils.isEmpty(userAuthToken)) {
                connection.setRequestProperty(X_AUTHORIZATION_HEADER, userAuthToken)
            }
        } catch (e: Exception) {
            KmExceptionAnalytics.captureException(e)
            e.printStackTrace()
        } finally {
            isRefreshTokenInProgress = false
        }
    }
}