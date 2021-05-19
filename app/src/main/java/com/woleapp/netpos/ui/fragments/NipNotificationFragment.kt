package com.woleapp.netpos.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentNipNotificationsBinding
import com.woleapp.netpos.databinding.LayoutBankDetailsBinding
import com.woleapp.netpos.model.*
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.util.copyTextToClipboard
import com.woleapp.netpos.util.disposeWith
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class NipNotificationFragment : BaseFragment() {
    private lateinit var binding: FragmentNipNotificationsBinding
    private lateinit var adapter: ServiceAdapter
    private val compositeDisposable = CompositeDisposable()
    private lateinit var event: MqttEvent<NipEvent>

    companion object {
        fun newInstance(): NipNotificationFragment {
            return NipNotificationFragment()
        }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNipNotificationsBinding.inflate(inflater, container, false)
        adapter = ServiceAdapter {
            when (it.id) {
                0 -> {
                    Toast.makeText(requireContext(), "Please wait!!!", Toast.LENGTH_SHORT).show()
                    getCode()
                }
                1 -> addFragmentWithoutRemove(
                    NipNotificationListFragment.newInstance(
                        NipNotificationListFragment.LAST_TWO
                    )
                )
                2 -> {
                    showCalendarDialog()
                }

                3 -> addFragmentWithoutRemove(NipNotificationSearch())
            }
        }
        binding.rvTransactions.layoutManager = GridLayoutManager(context, 2)
        binding.rvTransactions.adapter = adapter
        setService()
        event = MqttEvent<NipEvent>()
            .apply {
                this.geo = "lat:51.507351-long:-0.127758"
            }

        return binding.root
    }

    private fun showCalendarDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, i, i2, i3 ->
                addFragmentWithoutRemove(
                    NipNotificationListFragment.newInstance(
                        NipNotificationListFragment.END_OF_DAY,
                        Calendar.getInstance().apply { set(i, i2, i3) }.timeInMillis
                    )
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun getCode() {
        StormApiClient.getInstance().getSessionCode().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { sessionCode, throwable ->
                sessionCode?.let {
                    event.apply {
                        this.event = MqttEvents.NIP_NEW.event
                        this.code = "00"
                        this.timestamp = System.currentTimeMillis()
                        this.data = NipEvent(session_code = it.sessionCode)
                        this.status = MqttStatus.SUCCESS.name
                    }
                    MqttHelper.sendPayload(MqttTopics.NIP_NEW, event)
                    showDialogForAccountTransfer(it.sessionCode)
                }
                throwable?.let {
                    event.apply {
                        this.event = MqttEvents.NIP_NEW.event
                        this.code = "99"
                        this.timestamp = System.currentTimeMillis()
                        this.status = MqttStatus.ERROR.name
                    }
                    MqttHelper.sendPayload(MqttTopics.NIP_NEW, event)
                    Timber.e("Nip Error: ${it.localizedMessage}")
                    Toast.makeText(
                        requireContext(),
                        "Nip Error: ${it.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .disposeWith(compositeDisposable)
    }

    private fun showDialogForAccountTransfer(code: String) {
        val bankDetailsBinding: LayoutBankDetailsBinding = LayoutBankDetailsBinding.inflate(
            layoutInflater, null, false
        )
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.SheetDialog)
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.setContentView(bankDetailsBinding.root)
        val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        val bank = "GTB"
        val accountNumber = "0597024646"
        val accountName = "NETPLUS/STORM"
//        val bank ="GTB"
//        val accountNumber = "0239952959"
//        val accountName = "EasyPOS"
        //val accountNumber2 = "2684362099"
        //val bank2 = "FCMB"
        val accountNumber2 = ""
        val bank2 = ""
        bankDetailsBinding.accountNumber2.text = accountNumber2
        bankDetailsBinding.bank2.text = bank2
        bankDetailsBinding.accountNumber.text = accountNumber
        bankDetailsBinding.bank.text = bank
        bankDetailsBinding.accountName.text = accountName
        val ref = "Please use the code $code as a reference during transfer or payment"
        bankDetailsBinding.reference.text = ref
        bankDetailsBinding.accountNumber.setOnClickListener {
            copyTextToClipboard(
                requireContext(),
                "Account Number",
                accountNumber
            )
            Toast.makeText(
                requireContext(),
                "Account number copied to clipboard",
                Toast.LENGTH_SHORT
            )
                .show()
        }
        bankDetailsBinding.accountNumber2.setOnClickListener {
            copyTextToClipboard(
                requireContext(),
                "Account Number",
                accountNumber2
            )
            Toast.makeText(
                requireContext(),
                "Account number copied to clipboard",
                Toast.LENGTH_SHORT
            ).show()
        }
        bankDetailsBinding.tap.setOnClickListener {
            copyTextToClipboard(requireContext(), "Reference", "" + ref)
            Toast.makeText(requireContext(), "Reference copied to clipboard", Toast.LENGTH_SHORT)
                .show()
        }
        bankDetailsBinding.btnDone.setOnClickListener {
            if (bottomSheetDialog.isShowing) bottomSheetDialog.cancel()
        }
        bottomSheetDialog.show()
    }

    private fun setService() {
        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "New", R.drawable.ic_add))
                add(Service(1, "Last Two Transfers", R.drawable.ic_write))
                add(Service(2, "End Of Day Notification", R.drawable.ic_end_of_day))
                add(Service(3, "Search with Reference", R.drawable.ic_search))
            }
        adapter.submitList(listOfService)
    }

}