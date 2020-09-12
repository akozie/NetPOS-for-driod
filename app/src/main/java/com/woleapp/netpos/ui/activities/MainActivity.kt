@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.danbamitale.epmslib.entities.clearPinKey
import com.danbamitale.epmslib.utils.TripleDES
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs

import com.socsi.aidl.pinservice.OperationPinListener
import com.socsi.smartposapi.gmalgorithm.Dukpt

import com.socsi.smartposapi.ped.Ped
import com.sunyard.i80.util.Util
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.ActivityMainBinding
import com.woleapp.netpos.model.User
import com.woleapp.netpos.nibss.CONFIGURATION_ACTION
import com.woleapp.netpos.nibss.CONFIGURATION_STATUS
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.ui.fragments.DashboardFragment
import com.woleapp.netpos.util.CardUtil
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.xorHex

import timber.log.Timber

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private var progressDialog: ProgressDialog? = null
    private lateinit var alertDialog: AlertDialog
    private lateinit var binding: ActivityMainBinding
    private val gson = Gson()

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

    private fun showAlertDialog() {
        alertDialog.apply {
            setMessage("Terminal Configuration Failed")
            show()
        }
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(CONFIGURATION_ACTION))
        if (NetPosTerminalConfig.isConfigurationInProcess)
            showProgressDialog()
        else if (NetPosTerminalConfig.configurationStatus == -1)
            NetPosTerminalConfig.init(this)
        //else
        //showPinpad("5199110748591994")

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
        showFragment(DashboardFragment(), DashboardFragment::class.java.simpleName)
        //showPinpad("5199110748591994")
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

    private fun showPinpad(
        pan: String,
        getPinHandler: com.socsi.smartposapi.emv2.EmvL2.GetPinHandler? = null,
        proceed: Boolean = false
    ) {
        val key = NetPosTerminalConfig.getKeyHolder()?.clearPinKey!!
        val dadada = TripleDES.encrypt("0425A8EF8B7A6E66", key)
        Timber.e("Test encrypt $dadada")
        val lalala = TripleDES.decrypt(dadada, key)
        Timber.e("Test decrypt $lalala")
        Timber.e("Pan passed to pin Handler: $pan")
        val param = CardUtil.buildParam(pan)
        //param.putInt(KeyBoardConstant.BUNDLE_KEY_KEYSTYPE, 1)
        val ped = Ped.getInstance()
        ped.loadMKey(Ped.KEY_TYPE_DES_KEY, key, true)
        ped.startPinInput(this, 1, param, object : OperationPinListener {
            override fun onInput(len: Int, key: Int) {
                showToastOnUiThread("Len: $len")
            }

            override fun onError(errorCode: Int) {
                showToastOnUiThread("Error: $errorCode")
                Timber.e("onError:::: errorCode:$errorCode")
            }

            override fun onConfirm(@Nullable data: ByteArray, @Nullable isNonePin: Boolean) {
                getPinHandler?.onGetPin(1, data)
                showToastOnUiThread("Confirm: ${Util.BytesToString(data)}")
                Timber.e("ContentValues ${Util.BytesToString(data)}")
//                    val encryptedPin = Ped.getInstance().encryptPIN(1, 1, 1, data, "")
//                    Timber.e(encryptedPin)
                Timber.e("Byte Array $data")
                val ssPin = "042539FFFFFFFFFF"
                val cardNum = "0000${pan.substring(3, 15)}"
                val hexed = xorHex(ssPin, cardNum)
                Timber.e("hexed: $hexed")
                Timber.e("encrypt: ${TripleDES.encrypt(hexed, key)}")
                val pin = Util.BytesToString(data)
                Timber.e("pin from pad: $pin")
                Timber.e(
                    pin
                )
                Timber.e("decrypted from pad: ${TripleDES.decrypt(pin, key)}")
//                    val encryptedPin = Ped.getInstance().encryptPIN(1,1,1, data, "")
//                    Timber.e("encryptedPin: $encryptedPin")
                //viewModel.setCardPin(pin)
//                    if (proceed)
//                        requireActivity().runOnUiThread {
//                            viewModel.makePayment(requireContext(), transactionType)
//                        }
            }

            override fun onCancel() {
                showToastOnUiThread("canceled")
                Timber.e("onCancel")
            }
        })
    }

    private fun showToastOnUiThread(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
