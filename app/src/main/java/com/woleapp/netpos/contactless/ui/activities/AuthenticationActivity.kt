package com.woleapp.netpos.contactless.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.ActivityAuthenticationBinding
import com.woleapp.netpos.contactless.nibss.NetPosTerminalConfig
import com.woleapp.netpos.contactless.ui.fragments.LoginFragment
import com.woleapp.netpos.contactless.util.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthenticationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthenticationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)
        if (RootUtil.isDeviceRooted){
            Toast.makeText(this, getString(R.string.device_is_rooted), Toast.LENGTH_SHORT).show()
            finish()
        }
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE),
                AppConstants.READ_PHONE_STATE_REQUEST_CODE
            )
        }
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
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == AppConstants.READ_PHONE_STATE_REQUEST_CODE) {
//            if (grantResults.size == 1 && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
//                val deviceSerialNumber = Build.getSerial()
//                Log.d("SERIAL", deviceSerialNumber)
//                Toast.makeText(this, "HEREEEEE", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "Not granted", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
}
