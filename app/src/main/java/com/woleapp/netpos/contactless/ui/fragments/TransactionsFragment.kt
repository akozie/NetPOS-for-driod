package com.woleapp.netpos.contactless.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.entities.TransactionType
import com.netpluspay.netposbarcodesdk.NetPosBarcodeSdk
import com.netpluspay.netposbarcodesdk.RESULT_CODE_TEXT
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.adapter.ServiceAdapter
import com.woleapp.netpos.contactless.databinding.FragmentTransactionsBinding
import com.woleapp.netpos.contactless.databinding.LayoutPreauthDialogBinding
import com.woleapp.netpos.contactless.model.Service
import com.woleapp.netpos.contactless.util.HISTORY_ACTION_PREAUTH
import com.woleapp.netpos.contactless.util.HISTORY_ACTION_REFUND
import com.woleapp.netpos.contactless.util.HISTORY_ACTION_REVERSAL

class TransactionsFragment : BaseFragment() {

    private lateinit var adapter: ServiceAdapter
    private lateinit var binding: FragmentTransactionsBinding
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    data?.let {
                        if (it.hasExtra(RESULT_CODE_TEXT)){
                            val text = it.getStringExtra(RESULT_CODE_TEXT)
                            Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{
                    Toast.makeText(requireContext(), "scan failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        adapter = ServiceAdapter {
            val nextFrag: Fragment? = when (it.id) {
                0 -> SalesFragment.newInstance()
                1 -> TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REFUND)
                8 -> TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REVERSAL)
                7 -> SalesFragment.newInstance(TransactionType.PURCHASE_WITH_CASH_BACK)
                2 -> {
                    showPreAuthDialog()
                    null
                }
                4 -> {
                    //NetPosBarcodeSdk.
                    //QRFragment()
                    NetPosBarcodeSdk.startScan(requireContext(), resultLauncher)
                    null
                }
                5 -> ReprintFragment()
                6 -> SalesFragment.newInstance(isVend = true)
                else -> SalesFragment.newInstance(TransactionType.CASH_ADVANCE)
            }
            nextFrag?.let { fragment ->
                addFragmentWithoutRemove(fragment)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTransactions.layoutManager = GridLayoutManager(context, 2)
        binding.rvTransactions.adapter = adapter
        setService()
    }

    private fun setService() {
        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "Purchase", R.drawable.ic_purchase))
                add(Service(7, "Purchase With Cashback", R.drawable.ic_purchase))
                //add(Service(1, "Refund", R.drawable.ic_loop))
                //add(Service(8, "Reversal", R.drawable.ic_loop))
                //add(Service(2, "PRE AUTHORIZATION", R.drawable.ic_pre_auth))
                add(Service(3, "Cash Advance", R.drawable.ic_pay_cash_icon))
                add(Service(4, "Scan QR", R.drawable.ic_qr_code))
                add(Service(5, "Reprint", R.drawable.ic_print))
                //add(Service(6, "VEND", R.drawable.ic_vend))
            }
        adapter.submitList(listOfService)
    }

    private fun showPreAuthDialog() {
        val dialog = AlertDialog.Builder(context)
            .apply {
                setCancelable(false)
            }.create()
        val preAuthDialogBinding =
            LayoutPreauthDialogBinding.inflate(LayoutInflater.from(context), null, false)
                .apply {
                    lifecycleOwner = viewLifecycleOwner
                    executePendingBindings()
                    preAuthNew.setOnClickListener {
                        dialog.dismiss()
                        addFragmentWithoutRemove(SalesFragment.newInstance(TransactionType.PRE_AUTHORIZATION))
                    }
                    preAuthComplete.setOnClickListener {
                        dialog.dismiss()
                        addFragmentWithoutRemove(
                            TransactionHistoryFragment.newInstance(
                                HISTORY_ACTION_PREAUTH
                            )
                        )
                    }
                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }
                }
        dialog.setView(preAuthDialogBinding.root)
        dialog.show()
    }
}