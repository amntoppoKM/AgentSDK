package io.kommunicate.agent.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast
import com.applozic.mobicommons.json.GsonUtils
import io.kommunicate.agent.MainActivity
import io.kommunicate.agent.applist.AppListActivity
import io.kommunicate.agent.databinding.FragmentSsoLoginBinding
import io.kommunicate.agent.model.Resource
import io.kommunicate.agent.repositories.KmAuthRepository
import io.kommunicate.agent.viewmodels.KmAuthViewModel
import java.util.regex.Pattern

class SSOLoginFragment : Fragment() {
    private var _binding: FragmentSsoLoginBinding? = null
    private val authViewModel by activityViewModels<KmAuthViewModel>()

    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSsoLoginBinding.inflate(inflater, container, false)

        binding.btnLogin.setOnClickListener {
            processSSOSignIn(binding.userIdEditText.text.toString(), null)
        }

        binding.kmBackToLogin.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        return binding.root
    }

    private fun processSSOSignIn(email: String, applicationId: String?) {
        authViewModel.initSamlLogin(email, applicationId).observe(viewLifecycleOwner, Observer {

            if (it.isSuccess()) {
                val intent = Intent(activity, AppListActivity::class.java)

                intent.putExtra(MainActivity.KM_USER_ID, binding.userIdEditText.text.toString())

                if (it.data?.applicationList == null) {
                    intent.putExtra(AppListActivity.APPLICATION_ID, applicationId)
                    intent.putExtra(AppListActivity.SAML_URL, it.data?.redirectionUrl)
                } else {
                    val applicationMapJson =
                            GsonUtils.getJsonFromObject(it.data.applicationList, Any::class.java)
                    intent.putExtra(MainActivity.KM_APP_LIST, applicationMapJson)
                    intent.putExtra(MainActivity.IS_SSO_LOGIN, true)
                }
                startActivity(intent)
                activity?.finish()
            } else {
                KmToast.error(context, it?.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}