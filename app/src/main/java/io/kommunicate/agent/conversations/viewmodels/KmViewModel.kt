package io.kommunicate.agent.conversations.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applozic.mobicomkit.uiwidgets.AlCustomizationSettings
import kotlinx.coroutines.CoroutineScope

open class KmViewModel(protected var alCustomizationSettings: AlCustomizationSettings) : ViewModel() {

    fun getScope(): CoroutineScope {
        return viewModelScope
    }
}