@file:Suppress("DEPRECATION")

package com.woleapp.netpos.contactless.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.danbamitale.epmslib.utils.IsoAccountType
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.visa.app.ttpkernel.ContactlessKernel
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.app.NetPosApp
import com.woleapp.netpos.contactless.databinding.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.contactless.model.QrTransactionResponseFinalModel
import com.woleapp.netpos.contactless.model.User
import com.woleapp.netpos.contactless.mqtt.MqttHelper
import com.woleapp.netpos.contactless.network.StormApiClient
import com.woleapp.netpos.contactless.nibss.NetPosTerminalConfig
import com.woleapp.netpos.contactless.receivers.BatteryReceiver
import com.woleapp.netpos.contactless.taponphone.mastercard.implementations.nfc.NFCManager.READER_FLAGS
import com.woleapp.netpos.contactless.taponphone.visa.LiveNfcTransReceiver
import com.woleapp.netpos.contactless.taponphone.visa.NfcPaymentType
import com.woleapp.netpos.contactless.ui.dialog.PasswordDialog
import com.woleapp.netpos.contactless.ui.fragments.DashboardFragment
import com.woleapp.netpos.contactless.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.contactless.util.AppConstants.IS_QR_TRANSACTION
import com.woleapp.netpos.contactless.util.AppConstants.WRITE_PERMISSION_REQUEST_CODE
import com.woleapp.netpos.contactless.util.Mappers.mapTransactionResponseToQrTransaction
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.showSnackBar
import com.woleapp.netpos.contactless.util.Singletons.gson
import com.woleapp.netpos.contactless.viewmodels.NfcCardReaderViewModel
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity @Inject constructor() :
    AppCompatActivity(),
    EasyPermissions.PermissionCallbacks,
    NfcAdapter.ReaderCallback {

    private lateinit var receiptPdf: File
    private var progressDialog: ProgressDialog? = null
    private lateinit var alertDialog: AlertDialog
    private lateinit var binding: ActivityMainBinding
    private lateinit var pdfView: LayoutPosReceiptPdfBinding
    private lateinit var qrPdfView: LayoutQrReceiptPdfBinding
    private lateinit var dialogContactlessReaderBinding: DialogContatclessReaderBinding
    private val viewModel by viewModels<NfcCardReaderViewModel>()
    private val contactlessKernel: ContactlessKernel by lazy {
        ContactlessKernel.getInstance(applicationContext)
    }

    private lateinit var waitingDialog: AlertDialog
    private val iFilter = IntentFilter().apply {
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(Intent.ACTION_BATTERY_LOW)
        addAction(Intent.ACTION_BATTERY_OKAY)
    }
    private val batteryReceiver = BatteryReceiver()
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var receiptDialogBinding: DialogTransactionResultBinding
    private lateinit var receiptAlertDialog: AlertDialog
    override fun onStop() {
        super.onStop()
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        unregisterReceiver(batteryReceiver)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(batteryReceiver, iFilter)
        // LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(CONFIGURATION_ACTION))
        when ( // NetPosTerminalConfig.isConfigurationInProcess -> showProgressDialog()
            NetPosTerminalConfig.configurationStatus
        ) {
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
        nfcAdapter = (applicationContext as NetPosApp).nfcProvider.mNFCManager?.mNfcAdapter
        if (nfcAdapter != null) {
            // Toast.makeText(this, "Device has NFC support", Toast.LENGTH_SHORT).show()
            if (nfcAdapter?.isEnabled == false) {
                AlertDialog.Builder(this)
                    .setTitle("NFC Message")
                    .setMessage("NFC is not enabled, goto device settings to enable")
                    .setCancelable(false)
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
                .setCancelable(false)
                .setMessage("Device dose not have NFC support")
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                    // finish()
                }.show()
        }
    }

    private fun startNfcPayment() {
        nfcAdapter?.enableReaderMode(
            this,
            this,
            READER_FLAGS,
            Bundle()
        )
    }

    private fun stopNfcPayment() {
        nfcAdapter?.disableReaderMode(this)
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
        Timber.e("resolve intent")
        // intent.action
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
        pdfView = LayoutPosReceiptPdfBinding.inflate(layoutInflater)
        qrPdfView = LayoutQrReceiptPdfBinding.inflate(layoutInflater)
        NetPosApp.INSTANCE.initMposLibrary(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        dialogContactlessReaderBinding =
            DialogContatclessReaderBinding.inflate(layoutInflater).apply {
                executePendingBindings()
            }
        waitingDialog = AlertDialog.Builder(this)
            .apply {
                setView(dialogContactlessReaderBinding.root)
                // setCancelable(false)
            }.create()
        receiptDialogBinding = DialogTransactionResultBinding.inflate(layoutInflater, null, false)
            .apply { executePendingBindings() }
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
        if (checkBillsPaymentToken().not()) {
            getBillsToken(StormApiClient.getInstance())
        }
        showFragment(DashboardFragment(), DashboardFragment::class.java.simpleName)
        viewModel.enableNfcForegroundDispatcher.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    startNfcPayment()
                } else {
                    stopNfcPayment()
                }
            }
        }
        viewModel.showAccountTypeDialog.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    showSelectAccountTypeDialog()
                }
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
                dialogContactlessReaderBinding.contactlessHeader.text =
                    getString(R.string.nfc_message, it.cardScheme)
                dialogContactlessReaderBinding.contactlessHeader.highlightTexts(
                    NfcPaymentType.MASTERCARD.cardScheme,
                    NfcPaymentType.VISA.cardScheme
                )
                dialogContactlessReaderBinding.cardScheme.setImageResource(it.icon)
                waitingDialog.show()
                return@observe
            }
            waitingDialog.dismissIfShowing()
        }
        viewModel.smsSent.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                receiptDialogBinding.progress.visibility = View.GONE
                receiptDialogBinding.sendButton.isEnabled = true
                if (it) {
                    Toast.makeText(this, "Sent Receipt", Toast.LENGTH_LONG).show()
                    receiptDialogBinding.telephone.text?.clear()
                    receiptAlertDialog.dismiss()
                }
            }
        }
        receiptAlertDialog = AlertDialog.Builder(this).setCancelable(false).apply {
            setView(receiptDialogBinding.root)
            receiptDialogBinding.apply {
                closeBtn.setOnClickListener {
                    receiptDialogBinding.telephone.text?.clear()
                    receiptAlertDialog.dismiss()
                }
                sendButton.setOnClickListener {
                    if (receiptDialogBinding.telephone.text.toString().length != 11) {
                        Toast.makeText(
                            this@MainActivity,
                            "Please enter a valid phone number",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    viewModel.sendSmS(
                        receiptDialogBinding.transactionContent.text.toString(),
                        receiptDialogBinding.telephone.text.toString()
                    )
                    progress.visibility = View.VISIBLE
                    sendButton.isEnabled = false
                }
            }
        }.create()
        receiptAlertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        viewModel.showQrPrintDialog.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                val qrTransaction = Gson().fromJson(it, QrTransactionResponseFinalModel::class.java)
                printQrTransactionUtil(qrTransaction)
            }
        }

        viewModel.showPrintDialog.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                when (Prefs.getString(PREF_PRINTER_SETTINGS, "nothing_is_there")) {
                    PREF_VALUE_PRINT_DOWNLOAD -> {
                        downloadPdfImpl()
                        showSnackBar(
                            binding.root,
                            getString(R.string.fileDownloaded)
                        )
                    }
                    PREF_VALUE_PRINT_SHARE -> {
                        downloadPdfImpl()
                        sharePdf(receiptPdf, this)
                    }
                    PREF_VALUE_PRINT_DOWNLOAD_AND_SHARE -> {
                        downloadPdfImpl()
                        showSnackBar(
                            binding.root,
                            getString(R.string.fileDownloaded)
                        )
                        sharePdf(receiptPdf, this)
                    }
                    else -> {
                        receiptAlertDialog.apply {
                            receiptDialogBinding.transactionContent.text = it
                            show()
                        }
                        receiptDialogBinding.apply {
                            progress.visibility = View.GONE
                            sendButton.isEnabled = true
                        }
                    }
                }
            }
        }
        waitingDialog.setOnDismissListener {
            viewModel.stopNfcReader()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                val token = task.result // this is the token retrieved
                Log.d("FCM", token)
            }
        )
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
                }
            }

            @Deprecated(
                "Deprecated from api",
                ReplaceWith("Check documentation, mfpm")
            )
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
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
        // locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }

    private fun showPinDialog(pan: String) {
        Timber.e("pan from show pin dialog")
        Timber.e(pan)
        PasswordDialog(
            this,
            pan,
            object : PasswordDialog.Listener {
                override fun onConfirm(pinBlock: String?) {
                    viewModel.setPinBlock(pinBlock)
                }

                override fun onError(message: String?) {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    viewModel.setIccCardHelperLiveData(ICCCardHelper(error = Throwable(message)))
                }
            }
        ).showDialog()
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

    override fun onTagDiscovered(tag: Tag?) {
        tag?.let {
            if (it.toString() == NFC_A_TAG || it.toString() == NFC_B_TAG) {
                runOnUiThread {
                    handleProvider(tag)
                    nfcAdapter?.disableReaderMode(this)
                }
            }
        }
    }

    private fun getPermissionAndCreatePdf(view: ViewDataBinding) {
        genericPermissionHandler(
            this@MainActivity,
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            WRITE_PERMISSION_REQUEST_CODE,
            getString(R.string.storage_permission_rationale_for_download)
        ) {
            receiptPdf = createPdf(view, this)
        }
    }

    private fun downloadPdfImpl() {
        viewModel.lastPosTransactionResponse.value?.let {
            if (it.TVR.contains(IS_QR_TRANSACTION)) {
                val qrTransaction = it.copy(TVR = it.TVR.replace(IS_QR_TRANSACTION, ""))
                    .mapTransactionResponseToQrTransaction()
                initViewsForPdfLayout(
                    qrPdfView,
                    qrTransaction
                )
                getPermissionAndCreatePdf(qrPdfView)
            } else {
                initViewsForPdfLayout(
                    pdfView,
                    viewModel.lastPosTransactionResponse.value
                )
                getPermissionAndCreatePdf(pdfView)
            }
        }
    }

    private fun downloadPflImplForQrTransaction(qrTransaction: QrTransactionResponseFinalModel) {
        initViewsForPdfLayout(
            qrPdfView,
            qrTransaction
        )
        getPermissionAndCreatePdf(qrPdfView)
    }

    private fun printQrTransactionUtil(qrTransaction: QrTransactionResponseFinalModel) {
        when (Prefs.getString(PREF_PRINTER_SETTINGS, "nothing_is_there")) {
            PREF_VALUE_PRINT_DOWNLOAD -> {
                downloadPflImplForQrTransaction(qrTransaction)
                showSnackBar(
                    binding.root,
                    getString(R.string.fileDownloaded)
                )
            }
            PREF_VALUE_PRINT_SHARE -> {
                downloadPflImplForQrTransaction(qrTransaction)
                sharePdf(receiptPdf, this)
            }
            PREF_VALUE_PRINT_DOWNLOAD_AND_SHARE -> {
                downloadPflImplForQrTransaction(qrTransaction)
                showSnackBar(
                    binding.root,
                    getString(R.string.fileDownloaded)
                )
                sharePdf(receiptPdf, this)
            }
            else -> {
                receiptAlertDialog.apply {
                    receiptDialogBinding.transactionContent.text =
                        qrTransaction.buildSMSTextForQrTransaction()
                    show()
                }
                receiptDialogBinding.apply {
                    progress.visibility = View.GONE
                    sendButton.isEnabled = true
                }
            }
        }
    }
}
