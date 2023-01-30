package io.kommunicate.agent.viewmodels


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applozic.mobicommons.ApplozicService
import io.kommunicate.agent.model.KmResult
import io.kommunicate.agent.model.KmSAMLResponse
import io.kommunicate.agent.model.Resource
import io.kommunicate.agent.model.SingleLiveEvent
import io.kommunicate.agent.repositories.KmAuthRepository
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class KmAuthViewModel : ViewModel() {
    private val authRepository: KmAuthRepository = KmAuthRepository(ApplozicService.getAppContext())
    private val initSamlLoginLiveData: SingleLiveEvent<Resource<KmSAMLResponse>> = SingleLiveEvent()


    fun initSamlLogin(email: String, applicationId: String?): MutableLiveData<Resource<KmSAMLResponse>> {
        if (email.isEmpty()) {
            initSamlLoginLiveData.postValue(Resource.error("Email field cannot be blank", null))
            return initSamlLoginLiveData;
        }
        if (!Pattern.compile(KmAuthRepository.EMAIL_REGEX).matcher(email)
                        .matches()) {
            initSamlLoginLiveData.postValue(Resource.error("Invalid email", null))
            return initSamlLoginLiveData;
        }
        viewModelScope.launch {
            val result = try {
                authRepository.initSamlLogin(email, applicationId)
            } catch (e: Exception) {
                e.printStackTrace()
                KmResult.Error(e)
            }

            initSamlLoginLiveData.postValue(Resource.success(result) as Resource<KmSAMLResponse>?)
            when (result) {
                is KmResult.Success<KmSAMLResponse> -> {
                    if (!result.data.message.isNullOrEmpty()) {
                        initSamlLoginLiveData.postValue(
                                Resource.error(
                                        result.data.message,
                                        null
                                )
                        )
                    } else {
                        initSamlLoginLiveData.postValue(Resource.success(result.data))
                    }
                }
                else -> initSamlLoginLiveData.postValue(
                        Resource.error(
                                "Unable init SAML login",
                                null
                        )
                )
            }
        }


        return initSamlLoginLiveData
    }
}