package com.woleapp.netpos.contactless.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.ActivityAuthenticationBinding
import com.woleapp.netpos.contactless.nibss.NetPosTerminalConfig
import com.woleapp.netpos.contactless.ui.fragments.LoginFragment
import com.woleapp.netpos.contactless.util.JWTHelper
import com.woleapp.netpos.contactless.util.PREF_AUTHENTICATED
import com.woleapp.netpos.contactless.util.PREF_USER_TOKEN
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthenticationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthenticationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)
        if (Prefs.getBoolean(PREF_AUTHENTICATED, false) && tokenValid()) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            // MqttHelper.init<Nothing>(applicationContext)
            NetPosTerminalConfig.init(applicationContext)
            finish()
        }
        showFragment(LoginFragment())
    }

    private fun tokenValid(): Boolean {
        val token = Prefs.getString(PREF_USER_TOKEN, null)
        return !(token.isNullOrEmpty() || JWTHelper.isExpired(token))
    }

    private fun showFragment(targetFragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .apply {
                    replace(
                        R.id.auth_container,
                        targetFragment,
                        targetFragment.javaClass.simpleName
                    )
                    setCustomAnimations(R.anim.right_to_left, android.R.anim.fade_out)
                    commit()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
