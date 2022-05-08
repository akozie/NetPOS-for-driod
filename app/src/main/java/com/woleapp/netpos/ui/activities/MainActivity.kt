@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.danbamitale.epmslib.utils.IsoAccountType
import com.pixplicity.easyprefs.library.Prefs
import com.visa.app.ttpkernel.ContactlessKernel
import com.woleapp.netpos.R
import com.woleapp.netpos.app.NetPosApp
import com.woleapp.netpos.databinding.ActivityMainBinding
import com.woleapp.netpos.databinding.DialogSelectAccountTypeBinding
import com.woleapp.netpos.model.User
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.receivers.BatteryReceiver
import com.woleapp.netpos.taponphone.visa.LiveNfcTransReceiver
import com.woleapp.netpos.ui.dialog.PasswordDialog
import com.woleapp.netpos.ui.fragments.DashboardFragment
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.gson
import com.woleapp.netpos.viewmodels.NfcCardReaderViewModel

import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private var progressDialog: ProgressDialog? = null
    private lateinit var alertDialog: AlertDialog
    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<NfcCardReaderViewModel>()
    private var nfcAdapter: NfcAdapter? = null
    private val contactlessKernel: ContactlessKernel by lazy {
        ContactlessKernel.getInstance(applicationContext)
    }

    private val waitingDialog: ProgressDialog by lazy {
        ProgressDialog(this)
            .apply {
                setMessage("Waiting for card")
                //setCancelable(false)
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

    override fun onStop() {
        super.onStop()
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        unregisterReceiver(batteryReceiver)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(batteryReceiver, iFilter)
        //LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(CONFIGURATION_ACTION))
        when (//NetPosTerminalConfig.isConfigurationInProcess -> showProgressDialog()
            NetPosTerminalConfig.configurationStatus) {
            -1 -> NetPosTerminalConfig.init(
                applicationContext
            )
            1 -> {
                dismissProgressDialogIfShowing()
//                NetPosTerminalConfig.getKeyHolder()?.let {
//                    NetPosSdk.writeTpkKey(
//                        DeviceConfig.TPKIndex,
//                        it.clearPinKey
//                    )
//                }
            }
        }
        checkTokenExpiry()
        val nfcManager: NfcManager = getSystemService(NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter
        if (nfcAdapter != null) {
            //Toast.makeText(this, "Device has NFC support", Toast.LENGTH_SHORT).show()
            if (nfcAdapter!!.isEnabled)
                Toast.makeText(this, "NFC enabled", Toast.LENGTH_SHORT).show()
            else {
                AlertDialog.Builder(this)
                    .setTitle("NFC Message")
                    .setMessage("NFC is not enabled, go device settings to enable")
                    .setPositiveButton("Settings") { dialog, _ ->
                        dialog.dismiss()
                        startActivityForResult(
                            Intent(android.provider.Settings.ACTION_NFC_SETTINGS),
                            0
                        )
                    }.show()
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("NFC Message")
                .setMessage("Device dose not have NFC support")
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                    // finish()
                }.show()
        }
    }

    private fun startNfcPayment() {
        val pendingIntent =PendingIntent.getActivity(
            this, 0,
            Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, NFC_INTENT_FILTER, TECH_LIST)
    }

    private fun stopNfcPayment() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        Timber.e("on new intent")
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let {
            resolveIntent(it)
        }
    }


    private fun resolveIntent(intent: Intent) {
        Timber.e("reaolve intent")
        //intent.action
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag
        tag?.let {
            if (it.toString() == NFC_A_TAG || it.toString() == NFC_B_TAG) {
                handleProvider(tag)
            }
        }
    }

    private fun handleProvider(tag: Tag) {
        val mTagCom = IsoDep.get(tag)
        try {
            mTagCom.connect()
            val logger = StringBuilder()
            val nfcTransReceiver = LiveNfcTransReceiver(logger, mTagCom)
            viewModel.doVisaTransaction(nfcTransReceiver, contactlessKernel)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun logout() {
        Prefs.remove(PREF_USER_TOKEN)
        Prefs.remove(PREF_AUTHENTICATED)
        Prefs.remove(PREF_KEYHOLDER)
        Prefs.remove(PREF_CONFIG_DATA)
        Prefs.remove(PREF_USER)
        MqttHelper.disconnect()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetPosApp.INSTANCE.initMposLibrary(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        //loadCerts()
        if (!EasyPermissions.hasPermissions(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            EasyPermissions.requestPermissions(
                this,
                "Accept Location and Storage Permissions",
                1,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
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
        if (checkBillsPaymentToken().not())
            getBillsToken(StormApiClient.getInstance())
        showFragment(DashboardFragment(), DashboardFragment::class.java.simpleName)
        viewModel.enableNfcForegroundDispatcher.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    startNfcPayment()
                else
                    stopNfcPayment()
            }
        }
        viewModel.showAccountTypeDialog.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    showSelectAccountTypeDialog()
            }
        }
        viewModel.showPinPadDialog.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                Timber.e("show pin dialog")
                showPinDialog(it)
            }
        }
        viewModel.showWaitingDialog.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    waitingDialog.show()
                else waitingDialog.dismiss()
            }
        }
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
            override fun onLocationChanged(location: Location) {
                // Called when a new location is found by the network location provider.
                location.let {
                    Prefs.putString(PREF_LAST_LOCATION, "lat:${it.latitude} long:${it.longitude}")
                    //Timber.e("lat:${it.latitude} long:${it.longitude}")
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Timber.e("On status changed")
            }

            override fun onProviderEnabled(provider: String) {
                Timber.e("On Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
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

    private fun showPinDialog(pan: String) {
        Timber.e("pan from show pin dialog")
        Timber.e(pan)
        PasswordDialog(this, pan, object : PasswordDialog.Listener {
            override fun onConfirm(pinBlock: String?) {
                viewModel.setPinBlock(pinBlock)
            }

            override fun onError(message: String?) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                viewModel.setIccCardHelperLiveData(ICCCardHelper(error = Throwable(message)))
            }

        }).showDialog()
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

    private fun showSelectAccountTypeDialog() {
        var dialogSelectAccountTypeBinding: DialogSelectAccountTypeBinding
        val dialog = AlertDialog.Builder(this)
            .apply {
                dialogSelectAccountTypeBinding =
                    DialogSelectAccountTypeBinding.inflate(
                        LayoutInflater.from(context),
                        null,
                        false
                    )
                        .apply {
                            executePendingBindings()
                        }
                setView(dialogSelectAccountTypeBinding.root)
                setCancelable(false)
            }.create()
        dialogSelectAccountTypeBinding.accountTypes.setOnCheckedChangeListener { _, checkedId ->
            val accountType = when (checkedId) {
                R.id.savings_account -> IsoAccountType.SAVINGS
                R.id.current_account -> IsoAccountType.CURRENT
                R.id.credit_account -> IsoAccountType.CREDIT
                R.id.bonus_account -> IsoAccountType.BONUS_ACCOUNT
                R.id.investment_account -> IsoAccountType.INVESTMENT_ACCOUNT
                R.id.universal_account -> IsoAccountType.UNIVERSAL_ACCOUNT
                else -> IsoAccountType.DEFAULT_UNSPECIFIED
            }
            dialog.dismiss()
            Timber.e("$checkedId")
            viewModel.iccCardHelper.apply {
                this.accountType = accountType
            }
            viewModel.finishNfcReading()
        }
        dialogSelectAccountTypeBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
            viewModel.setIccCardHelperLiveData(ICCCardHelper(error = Throwable("Operation was canceled")))
        }
        dialog.show()
    }
}
