package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentTransactionsBinding
import com.woleapp.netpos.databinding.QrAmoutDialogBinding
import com.woleapp.netpos.databinding.QrBottomSheetDialogBinding
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.viewmodels.QRViewModel

class QRFragment : BaseFragment() {

    private lateinit var adapter: ServiceAdapter
    private lateinit var binding: FragmentTransactionsBinding
    private val viewModel by viewModels<QRViewModel>()
    private lateinit var masterpassQrBottomSheetDialogBinding: QrBottomSheetDialogBinding
    private lateinit var masterPassQrBottomSheetDialog: BottomSheetDialog
    private lateinit var nibssQrBottomSheetDialogBinding: QrBottomSheetDialogBinding
    private lateinit var nibssQrBottomSheetDialog: BottomSheetDialog
    private lateinit var qrAmoutDialogBinding: QrAmoutDialogBinding
    private lateinit var qrAmountDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        binding.rvTransactionsHeader.text = getString(R.string.qr_payment)
        adapter = ServiceAdapter {
            showAmountDialog(it.id)
        }
        masterpassQrBottomSheetDialogBinding =
            QrBottomSheetDialogBinding.inflate(
                layoutInflater, null, false
            ).apply {
                executePendingBindings()
                lifecycleOwner = viewLifecycleOwner
            }
        masterPassQrBottomSheetDialog =
            BottomSheetDialog(requireContext(), R.style.SheetDialog).apply {
                setCancelable(false)
                setContentView(masterpassQrBottomSheetDialogBinding.root)
                masterpassQrBottomSheetDialogBinding.closeBtn.setOnClickListener { this.cancel() }
            }
        nibssQrBottomSheetDialogBinding =
            QrBottomSheetDialogBinding.inflate(
                layoutInflater, null, false
            ).apply {
                executePendingBindings()
                lifecycleOwner = viewLifecycleOwner
            }
        nibssQrBottomSheetDialog = BottomSheetDialog(requireContext(), R.style.SheetDialog).apply {
            setCancelable(false)
            setContentView(nibssQrBottomSheetDialogBinding.root)
            nibssQrBottomSheetDialogBinding.closeBtn.setOnClickListener { this.cancel() }
        }
        qrAmoutDialogBinding = QrAmoutDialogBinding.inflate(inflater, null, false)
            .apply {
                executePendingBindings()
                lifecycleOwner = viewLifecycleOwner
            }
        qrAmountDialog = AlertDialog.Builder(requireContext()).apply {
            setView(qrAmoutDialogBinding.root)
        }.create()
        viewModel.masterPassQrBitmap.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { bitmap ->
                masterpassQrBottomSheetDialogBinding.scanToPay.visibility = View.VISIBLE
                masterpassQrBottomSheetDialogBinding.progress.visibility = View.GONE
                masterpassQrBottomSheetDialogBinding.providerQr.visibility = View.VISIBLE
                masterpassQrBottomSheetDialogBinding.qr.setImageBitmap(bitmap)
            }
        }
        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.qrErrorMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                masterpassQrBottomSheetDialogBinding.scanToPay.visibility = View.GONE
                masterpassQrBottomSheetDialogBinding.progress.visibility = View.GONE
                masterpassQrBottomSheetDialogBinding.errorMessage.text = it
            }
        }
        viewModel.nibssQRBitmap.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                nibssQrBottomSheetDialogBinding.qr.setImageBitmap(it)
                nibssQrBottomSheetDialogBinding.scanToPay.visibility = View.VISIBLE
                nibssQrBottomSheetDialogBinding.progress.visibility = View.GONE
                startNibssReQueryTimer()
            }
        }
        viewModel.reQuerying.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    nibssQrBottomSheetDialogBinding.progress.visibility = View.VISIBLE
                    nibssQrBottomSheetDialogBinding.scanToPay.text = getString(R.string.req_trans)
                } else {
                    nibssQrBottomSheetDialogBinding.progress.visibility = View.GONE
                    if (viewModel.stillHasRetryAttempts)
                        startNibssReQueryTimer()
                }
            }
        }
        return binding.root
    }

    private fun startNibssReQueryTimer() {
        val timer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                nibssQrBottomSheetDialogBinding.scanToPay.text = getString(
                    R.string.requerying_qr_transaction,
                    millisUntilFinished / 1000
                )
            }

            override fun onFinish() {

            }
        }
        timer.start()
    }

    private fun showAmountDialog(serviceId: Int) {
        qrAmountDialog.show()
        qrAmoutDialogBinding.proceed.setOnClickListener {
            val amountDouble = qrAmoutDialogBinding.amount.text.toString().toDoubleOrNull()
            amountDouble?.let {
                qrAmountDialog.cancel()
                when (serviceId) {
                    0 -> showMasterPassQRBottomSheetDialog(it)
                    1 -> showNibssQRBottomSheet(it)
                }
            }
        }
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
                add(Service(0, "MasterPass QR", R.drawable.masterpass))
                add(Service(1, "NIBSS QR", R.drawable.ic_qr_code))
            }
        adapter.submitList(listOfService)
    }

    private fun showMasterPassQRBottomSheetDialog(amount: Double) {
        masterpassQrBottomSheetDialogBinding.progress.visibility = View.VISIBLE
        masterpassQrBottomSheetDialogBinding.providerQr.visibility = View.GONE
        masterpassQrBottomSheetDialogBinding.qr.setImageBitmap(null)
        masterPassQrBottomSheetDialog.show()
        viewModel.getMasterPassQr(amount)
    }

    private fun showNibssQRBottomSheet(amount: Double) {
        nibssQrBottomSheetDialogBinding.progress.visibility = View.VISIBLE
        nibssQrBottomSheetDialogBinding.providerQr.visibility = View.GONE
        nibssQrBottomSheetDialogBinding.qr.setImageBitmap(null)
        nibssQrBottomSheetDialog.show()
        viewModel.getNibssQR(amount)
    }
}