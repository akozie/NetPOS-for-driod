package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.entities.TransactionType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonObject
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentTransactionsBinding
import com.woleapp.netpos.databinding.LayoutPreauthDialogBinding
import com.woleapp.netpos.databinding.QrBottomSheetDialogBinding
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.util.HISTORY_ACTION_PREAUTH
import com.woleapp.netpos.util.HISTORY_ACTION_REFUND
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.util.disposeWith
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.qr_bottom_sheet_dialog.*
import timber.log.Timber

class TransactionsFragment : BaseFragment() {

    private lateinit var adapter: ServiceAdapter
    private lateinit var binding: FragmentTransactionsBinding

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
                2 -> {
                    showPreAuthDialog()
                    null
                }
                4 -> {
                    //showQRBottomSheetDialog()
                    QRFragment()
                }
                5 -> ReprintFragment()
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
                add(Service(0, "Sales", R.drawable.ic_purchase))
                add(Service(1, "Refund", R.drawable.ic_loop))
                add(Service(2, "PRE AUTHORIZATION", R.drawable.ic_pre_auth))
                add(Service(3, "Cash Advance", R.drawable.ic_pay_cash_icon))
                add(Service(4, "QR", R.drawable.ic_qr_code))
                add(Service(5, "Reprint", R.drawable.ic_print))
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