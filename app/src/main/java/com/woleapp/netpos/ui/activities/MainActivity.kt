@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.ActivityMainBinding
import com.woleapp.netpos.model.User
import com.woleapp.netpos.nibss.CONFIGURATION_ACTION
import com.woleapp.netpos.nibss.CONFIGURATION_STATUS
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.receivers.BatteryReceiver
import com.woleapp.netpos.ui.fragments.DashboardFragment
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.gson
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private var progressDialog: ProgressDialog? = null
    private lateinit var alertDialog: AlertDialog
    private lateinit var binding: ActivityMainBinding

    //private lateinit var client: MqttAndroidClient
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.getIntExtra(CONFIGURATION_STATUS, -1)) {
                    0 -> showProgressDialog()
                    1 -> {
                        Toast.makeText(context!!, "Terminal Configured", Toast.LENGTH_LONG).show()
                        dismissProgressDialogIfShowing()
                        //showPinpad("5199110748591994")
                    }
                    -1 -> {
                        dismissProgressDialogIfShowing()
                        showAlertDialog()
                    }
                }
            }
        }

    }
    private val iFilter = IntentFilter().apply {
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(Intent.ACTION_BATTERY_LOW)
        addAction(Intent.ACTION_BATTERY_OKAY)
    }
    private val batteryReceiver = BatteryReceiver()
    private fun showAlertDialog() {
        alertDialog.apply {
            setMessage("Terminal Configuration Failed, You won't be able to make any transactions, if the problem persists contact an Administrator")
            show()
        }
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        unregisterReceiver(batteryReceiver)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(batteryReceiver, iFilter)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(CONFIGURATION_ACTION))
        if (NetPosTerminalConfig.isConfigurationInProcess)
            showProgressDialog()
        else if (NetPosTerminalConfig.configurationStatus == -1)
            NetPosTerminalConfig.init(applicationContext)

        checkTokenExpiry()
    }

    private fun logout(){
        Prefs.remove(PREF_USER)
        Prefs.remove(PREF_USER_TOKEN)
        Prefs.remove(PREF_AUTHENTICATED)
        Prefs.remove(PREF_KEYHOLDER)
        Prefs.remove(PREF_CONFIG_DATA)
        val intent = Intent(this, AuthenticationActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkTokenExpiry() {
        val token = Prefs.getString(PREF_USER_TOKEN, null)
        token?.let {
            if (JWTHelper.isExpired(it)) {
                logout()
            }
        }
    }

    private fun dismissProgressDialogIfShowing() {
        progressDialog?.dismiss()
    }

    private fun showProgressDialog() {
        progressDialog?.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        if (!EasyPermissions.hasPermissions(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            EasyPermissions.requestPermissions(
                this,
                "Accept Location Permissions",
                1,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        progressDialog = ProgressDialog(this).apply {
            setMessage("Configuring Terminal, Please wait")
            setCancelable(false)
        }
        alertDialog = AlertDialog.Builder(this).run {
            setCancelable(false)
            title = "Message"
            setPositiveButton("Retry") { dialog, _ ->
                NetPosTerminalConfig.init(applicationContext)
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                Toast.makeText(applicationContext, "Configuration cancelled", Toast.LENGTH_LONG)
                    .show()
                dialog.dismiss()
            }
            create()
        }
        val user = gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        binding.dashboardHeader.username.text = user.business_name
        binding.dashboardHeader.logout.setOnClickListener {
            logout()
        }
        showFragment(DashboardFragment(), DashboardFragment::class.java.simpleName)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        getLocationUpdates()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

    }

    @SuppressLint("MissingPermission")
    private fun getLocationUpdates() {
        val locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                // Called when a new location is found by the network location provider.
                location?.let {
                    Prefs.putString(PREF_LAST_LOCATION, "lat:${it.latitude} long:${it.longitude}")
                    Timber.e("lat:${it.latitude} long:${it.longitude}")
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Timber.e("On status changed")
            }

            override fun onProviderEnabled(provider: String?) {
                Timber.e("On Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String?) {
                Timber.e("On Provider disabled $provider")
            }
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0f,
            locationListener
        )
        //locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }

    private fun showFragment(targetFragment: Fragment, className: String) {
        try {
            supportFragmentManager.beginTransaction()
                .apply {
                    replace(R.id.container_main, targetFragment, className)
                    setCustomAnimations(R.anim.right_to_left, android.R.anim.fade_out)
                    commit()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
