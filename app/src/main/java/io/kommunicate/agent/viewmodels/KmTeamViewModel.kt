package io.kommunicate.agent.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applozic.mobicommons.ApplozicService
import io.kommunicate.agent.model.KmResult
import io.kommunicate.agent.model.KmTeam
import io.kommunicate.agent.model.Resource
import io.kommunicate.agent.repositories.KmTeamRepository
import kotlinx.coroutines.launch

class KmTeamViewModel : ViewModel() {
    private val teamRepository: KmTeamRepository = KmTeamRepository(ApplozicService.getAppContext())

    private val teamsLiveData: MutableLiveData<Resource<List<KmTeam>>> = MutableLiveData()
    private val teamDetailsLiveData: MutableLiveData<Resource<KmTeam>> = MutableLiveData()

    companion object TeamsCache {
        private val teamList: MutableList<KmTeam> = ArrayList()
        private var defaultTeamData: KmTeam? = null

        fun getTeamListFromCache(): List<KmTeam> {
            return teamList
        }

        fun addTeamListToCache(newTeamList: List<KmTeam>) {
            if (teamList.isNotEmpty()) {
                teamList.clear()
            }
            teamList.addAll(newTeamList)
        }

        fun clearTeams() {
            teamList.clear()
        }

        fun getDefaultTeamDataCache(): KmTeam? {
            return defaultTeamData
        }

        fun setDefaultTeamData(newdefaultTeamData: KmTeam) {
            defaultTeamData = newdefaultTeamData
        }
    }


    fun getTeamList(): MutableLiveData<Resource<List<KmTeam>>> {
        when (getTeamListFromCache().isNullOrEmpty()) {
            false -> {
                teamsLiveData.postValue(Resource.success(getTeamListFromCache()))
            }

            true -> {
                viewModelScope.launch {
                    val result = try {
                        teamRepository.getTeamListResponse()
                    } catch (e: Exception) {
                        KmResult.Error(e)
                    }

                    when (result) {
                        is KmResult.Success<List<KmTeam>> -> {
                            addTeamListToCache(result.data)
                            teamsLiveData.postValue(Resource.success(getTeamListFromCache()))
                        }

                        else -> teamsLiveData.postValue(Resource.error("Unable to fetch teams", null))
                    }
                }
            }
        }
        return teamsLiveData
    }

    fun getDefaultTeamDetails(): MutableLiveData<Resource<KmTeam>> {
        when (getDefaultTeamDataCache() == null) {
            false -> {
                teamDetailsLiveData.postValue(Resource.success(getDefaultTeamDataCache()))
            }

            true -> {
                viewModelScope.launch {
                    val result = try {
                        teamRepository.getTeamDataResponse(KmTeamRepository.Urls.DEFAULT_TEAM_NAME)
                    } catch (e: Exception) {
                        KmResult.Error(e)
                    }

                    when (result) {
                        is KmResult.Success<KmTeam> -> {
                            setDefaultTeamData(result.data)
                            teamDetailsLiveData.postValue(Resource.success(getDefaultTeamDataCache()))
                        }
                        else -> teamDetailsLiveData.postValue(Resource.error("Unable to fetch teams", null))
                    }
                }
            }
        }
        return teamDetailsLiveData
    }
}