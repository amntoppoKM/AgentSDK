package io.kommunicate.agent.conversations.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applozic.mobicommons.ApplozicService
import io.kommunicate.agent.asyncs.AgentGetStatusTask.AgentDetail.RoleType
import io.kommunicate.agent.conversations.repositories.KmAppSettingsRepository
import io.kommunicate.agent.model.*
import io.kommunicate.models.KmAppSettingModel
import kotlinx.coroutines.launch

class KmAppSettingsViewModel : ViewModel() {
    private val appSettingsRepository: KmAppSettingsRepository =
        KmAppSettingsRepository(ApplozicService.getAppContext())
    private val appSettingsLiveData: MutableLiveData<Resource<KmAppSettingModel>> = MutableLiveData()
    val TAG = "APP_SETTING_VIEW_MODEL"

    companion object AppSettingsCache {
        var allowTeamMateAssignment = false
        var allowBotAssignment = false
        var allowTeamAssignment = false
        var currentUserRole: Short = RoleType.AGENT.value
        var appSetting: KmAppSettingModel? = null
        var isTrialExpired: Boolean = false

        fun isAssignmentToOthersAllowed(): Boolean {
            return (allowTeamMateAssignment || allowBotAssignment || allowTeamAssignment)
        }

        fun setCompanySetting(companySettings: KmAppSettingModel.KmCompanySetting) {
            if (companySettings.rolesAndPermissions != null) {
                allowBotAssignment = companySettings.rolesAndPermissions.isBotAssignmentAllowed!!
                allowTeamAssignment = companySettings.rolesAndPermissions.isTeamAssignmentAllowed!!
                allowTeamMateAssignment =
                    companySettings.rolesAndPermissions.isTeamMateAssignmentAllowed!!
            }
        }

        fun setAppSettingData(appSetting: KmAppSettingModel) {
            this.appSetting = appSetting;
            isTrialExpired = appSetting.isSuccess && appSetting.response.subscriptionDetails.isTrialExpired
            appSetting.response.companySetting?.let {setCompanySetting(appSetting.response.companySetting) }
        }

        fun getAppSettingData(): KmAppSettingModel? {
            return appSetting
        }

        fun saveCurrentUserRole(role: Short) {
            currentUserRole = role
        }

        fun isOperator(): Boolean {
            return currentUserRole == RoleType.OPERATOR.value
        }

        fun clearAppSettings() {
            appSetting = null
        }
    }

    fun fetchAppSettings(): MutableLiveData<Resource<KmAppSettingModel>> {
        when(getAppSettingData() == null) {
            false -> {
                appSettingsLiveData.postValue(Resource.success(getAppSettingData()))
            }
            true -> {
                viewModelScope.launch {
                    val result = try {
                        appSettingsRepository.getAppSettings()
                    } catch (e: Exception) {
                        KmResult.Error(e)
                    }
                    Log.i("App Settings VM", "response$result")

                    when (result) {
                        is KmResult.Success<KmAppSettingModel> -> {
                            setAppSettingData(result.data)
                            appSettingsLiveData.postValue(Resource.success(getAppSettingData()))
                        }
                        else -> {
                            Log.i(TAG, "Failed to fetch App Settings")
                        }
                    }
                }
            }
        }
        return appSettingsLiveData;
    }


}