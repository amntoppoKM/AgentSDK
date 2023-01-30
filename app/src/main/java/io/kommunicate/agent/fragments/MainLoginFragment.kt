package io.kommunicate.agent.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast
import com.applozic.mobicommons.commons.core.utils.Utils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import io.kommunicate.agent.LoginListener
import io.kommunicate.agent.R
import io.kommunicate.agent.applist.KmForgotPasswordActivity
import io.kommunicate.agent.databinding.FragmentMainLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*

private var signUpUrl = "https://dashboard.kommunicate.io/signup"
private const val RC_SIGN_IN = 100

class MainLoginFragment : Fragment() {
    private var _binding: FragmentMainLoginBinding? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null

    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMainLoginBinding.inflate(inflater, container, false)

        binding.showPassword.setOnClickListener {
            binding.passwordEditText.transformationMethod = if (isShowPassword()) null else PasswordTransformationMethod()
            binding.passwordEditText.textDirection = View.LAYOUT_DIRECTION_LOCALE
            binding.showPassword.text = Utils.getString(context, if (isShowPassword()) R.string.hide_password else R.string.show_password)
        }

        binding.singupInfo.setOnClickListener {
            if (!signUpUrl.startsWith("http://") && !signUpUrl.startsWith("https://")) {
                signUpUrl = "http://$signUpUrl"
            }
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(signUpUrl))
            startActivity(browserIntent)
        }

        binding.forgotPassword.setOnClickListener {
            val intent = Intent(activity, KmForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        handleLoginClicks()

        return binding.root
    }

    private fun isShowPassword(): Boolean {
        return binding.showPassword.text.toString() == Utils.getString(context, R.string.show_password)
    }

    private fun handleLoginClicks() {
        binding.btnLogin.setOnClickListener {
            if (binding.userIdEditText.text.isNullOrEmpty() || binding.passwordEditText.text.isNullOrEmpty()) {
                KmToast.error(context, R.string.km_email_or_pwd_field_blank_error, Toast.LENGTH_SHORT).show()
            } else {
                (activity as LoginListener).processAppList(binding.userIdEditText.text.toString(), binding.passwordEditText.text.toString(), false)
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            processGoogleSignIn()
        }

        binding.btnSsoLogin.setOnClickListener {
            (activity as LoginListener).openSSOPage()
        }
    }

    private fun processGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build()
        mGoogleSignInClient = context?.let { GoogleSignIn.getClient(it, gso) }

        if (GoogleSignIn.getLastSignedInAccount(context) != null) {
            mGoogleSignInClient?.signOut()?.addOnCompleteListener {
                startActivityForResult(mGoogleSignInClient?.signInIntent, RC_SIGN_IN)
            }
        } else {
            startActivityForResult(mGoogleSignInClient?.signInIntent, RC_SIGN_IN)
        }
    }

    private  fun verifyGoogleToken(account: GoogleSignInAccount) {
        val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
                .setAudience(Collections.singletonList(getString(R.string.server_client_id)))
                .build()

        var googleIdToken: GoogleIdToken? = null
        GlobalScope.launch { try {
            //googleIdToken = verifier.verify(account.getIdToken())
            googleIdToken = withContext(Dispatchers.IO) {
                verifier.verify(account.getIdToken())
            }
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
            if (googleIdToken != null) {
                val payload = googleIdToken!!.getPayload();
                val email = payload.getEmail();
                launch(Dispatchers.Main) {
                    (activity as LoginListener).processAppList(email, "", true)
                }

            } }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    verifyGoogleToken(account)
                }
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}