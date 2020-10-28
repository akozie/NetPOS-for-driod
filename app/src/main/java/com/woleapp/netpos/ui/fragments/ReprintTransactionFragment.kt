package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.netplus.sunyardlib.ReceiptBuilder
import com.woleapp.netpos.databinding.FragmentReprintTransactionsBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ReprintTransactionFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentReprintTransactionsBinding.inflate(inflater, container, false)
        val p = ReceiptBuilder()
            .apply {
                appendAID("sample aid")
                appendAddress("Oluwatayo Adegboye")
                appendAmount("5000")
                appendAppName("NetPOS")
                appendAppVersion("1.5")
                appendAuthorizationCode("Auth code")
                appendCardHolderName("card holder name")
                appendCardNumber("card number")
                appendCardScheme("card scheme")
                appendDateTime("Date time")
                appendRRN("Sample RRN")
                appendStan("Stan")
                appendTerminalId("Terminal ID")
                appendTransactionType("Trans New")
                appendTransactionStatus("status")
                appendResponseCode("00")
            }.print().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t, c ->

            }
        return binding.root
    }
}