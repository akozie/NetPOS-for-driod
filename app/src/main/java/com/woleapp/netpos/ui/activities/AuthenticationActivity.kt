package com.woleapp.netpos.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.ActivityAuthenticationBinding
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.ui.fragments.LoginFragment
import com.woleapp.netpos.util.PREF_AUTHENTICATED


class AuthenticationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthenticationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)
        if (Prefs.getBoolean(PREF_AUTHENTICATED, false)) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            MqttHelper.init()
            NetPosTerminalConfig.init(applicationContext)
            finish()
        }
        showFragment(LoginFragment())
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
