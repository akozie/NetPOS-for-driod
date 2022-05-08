@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.app.ProgressDialog

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServicesSpinnerAdapter
import com.woleapp.netpos.database.DataGenerator

import com.woleapp.netpos.databinding.*
import com.woleapp.netpos.model.Biller
import com.woleapp.netpos.model.ErrorNetworkResponse
import com.woleapp.netpos.model.SuccessNetworkResponse
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.network.StormUtilitiesApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.*
import com.woleapp.netpos.viewmodels.NfcCardReaderViewModel
import com.woleapp.netpos.viewmodels.UtilitiesViewModel
import timber.log.Timber


class UtilitiesPaymentFragment : BaseFragment() {

    private lateinit var binding: LayoutPowerOrElectricityBinding
    private lateinit var cableBinding: LayoutCableTvBinding
    private lateinit var internetSubscriptionBinding: LayoutInternetSubscriptionBinding
    private lateinit var airtimeOrDataBinding: LayoutAirtimeOrDataBinding
    private val viewModel by viewModels<UtilitiesViewModel>()
    private val nfcCardReaderViewModel by activityViewModels<NfcCardReaderViewModel>()
    private var progressDialog: ProgressDialog? = null
    private var alertDialog: AlertDialog? = null
    private var verifyBillDialog: BottomSheetDialog? = null
    private lateinit var receiptDialog: AlertDialog
    private lateinit var receiptDialogBinding: DialogTransactionResultBinding

    companion object {
        @JvmStatic
        fun newInstance(serviceId: Int): UtilitiesPaymentFragment {
            val bundle = Bundle()
            bundle.putInt("service_id", serviceId)
            val electricityPaymentFragment = UtilitiesPaymentFragment()
            electricityPaymentFragment.arguments = bundle
            return electricityPaymentFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (checkBillsPaymentToken().not()) getBillsToken(StormApiClient.getInstance())
        val dialogUtilitiesBinding = DialogBinding.inflate(inflater, null, false)
            .apply {
                tvYes.text = getString(R.string.lbl_continue)
                responseIcon.visibility = View.VISIBLE
                tvNo.visibility = View.GONE
            }
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
            .apply {
                setView(dialogUtilitiesBinding.root)
                setCancelable(false)
            }
        progressDialog = ProgressDialog(requireContext()).apply {
            setCancelable(false)
            this.setMessage("Please Wait")
        }
        receiptDialogBinding = DialogTransactionResultBinding.inflate(inflater, null, false)
            .apply { executePendingBindings() }

        val layoutVerifyUtilityPaymentBinding =
            LayoutVerifyUtilityPaymentBinding.inflate(inflater, null, false)
        layoutVerifyUtilityPaymentBinding.lifecycleOwner = viewLifecycleOwner
        layoutVerifyUtilityPaymentBinding.executePendingBindings()
        verifyBillDialog = BottomSheetDialog(requireContext(), R.style.SheetDialog)
            .apply { setContentView(layoutVerifyUtilityPaymentBinding.root) }
        layoutVerifyUtilityPaymentBinding.proceed.setOnClickListener {
            verifyBillDialog!!.dismiss()
            if (checkBillsPaymentToken().not()) {
                showProgressBar()
                getBillsToken(StormApiClient.getInstance()).observe(viewLifecycleOwner) { event ->
                    event.getContentIfNotHandled()?.let {
                        if (it) {
                            progressDialog?.setMessage("Processing")
                            viewModel.initiatePayment()
                        } else {
                            dismissProgressBar()
                            Toast.makeText(requireContext(), "Error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                progressDialog?.setMessage("Processing")
                viewModel.initiatePayment()
            }
        }

        viewModel.shouldRefreshNibssKeys.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    NetPosTerminalConfig.init(
                        requireContext().applicationContext,
                        configureSilently = true
                    )
            }
        }

        viewModel.initiateBillsPayment.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showCardDialog(
                    requireActivity(),
                    viewLifecycleOwner,
                ).observe(viewLifecycleOwner) { event ->
                    event.getContentIfNotHandled()?.let {
                        nfcCardReaderViewModel.initiateNfcPayment(10, 0, it)
                    }
                }
            }
        }

        nfcCardReaderViewModel.iccCardHelperLiveData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                it.error?.let { error ->
                    Timber.e(error)
                    Toast.makeText(
                        requireContext(),
                        error.localizedMessage,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                it.cardData?.let { _ ->
                    viewModel.setCardScheme(it.cardScheme!!)
                    viewModel.setCustomerName(it.customerName ?: "Customer")
                    viewModel.setAccountType(it.accountType!!)
                    viewModel.cardData = it.cardData
                    viewModel.makePayment(requireContext())
                }
            }
        }

        receiptDialog = AlertDialog.Builder(requireContext()).setCancelable(false).apply {
            setView(receiptDialogBinding.root)
            receiptDialogBinding.apply {
                closeBtn.setOnClickListener {
                    receiptDialog.dismiss()
                    //requireActivity().onBackPressed()
                }
                sendButton.setOnClickListener {
                    if (receiptDialogBinding.telephone.text.toString().length != 11) {
                        Toast.makeText(
                            requireContext(),
                            "Please enter a valid phone number",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    viewModel.sendSmS(
                        receiptDialogBinding.telephone.text.toString()
                    )
                    progress.visibility = View.VISIBLE
                    sendButton.isEnabled = false
                }
            }
        }.create()
        receiptDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        viewModel.showPrintDialog.observe(viewLifecycleOwner) { event ->
            Toast.makeText(requireContext(), "Show print dialog", Toast.LENGTH_SHORT).show()
            event.getContentIfNotHandled()?.let {
                receiptDialog.apply {
                    receiptDialogBinding.transactionContent.text = it
                    show()
                }
                receiptDialogBinding.apply {
                    progress.visibility = View.GONE
                    sendButton.isEnabled = true
                }
            }
        }

        viewModel.smsSent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                receiptDialogBinding.progress.visibility = View.GONE
                receiptDialogBinding.sendButton.isEnabled = true
                if (it) {
                    Toast.makeText(requireContext(), "Sent Receipt", Toast.LENGTH_LONG).show()
                    receiptDialog.dismiss()
                    //requireActivity().onBackPressed()
                }
            }
        }

        viewModel.showPrinterError.observe(viewLifecycleOwner) { event ->
            Toast.makeText(requireContext(), "Printer error", Toast.LENGTH_SHORT).show()
            event.getContentIfNotHandled()?.let {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .apply {
                        setTitle("Printer Error")
                        setIcon(R.drawable.ic_warning)
                        setMessage(it)
                        setPositiveButton("Send Receipt") { d, _ ->
                            d.dismiss()
                            viewModel.showReceiptDialog()
                        }
                        setNegativeButton("Dismiss") { d, _ ->
                            d.dismiss()
                            //requireActivity().onBackPressed()
                        }
                    }.show()
            }
        }

        alertDialog = alertDialogBuilder.create()
        val stormUtilitiesApiService =
            StormUtilitiesApiClient.getStormUtilitiesApiClientInstance(requireContext())
        viewModel.setStormUtilitiesApiService(stormUtilitiesApiService)

        Singletons.getCurrentlyLoggedInUser()!!.netplus_id
        viewModel.message.observe(
            viewLifecycleOwner
        ) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                progressDialog?.setMessage(it)
            }
        }
        viewModel.validateBillResponse.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                dismissProgressBar()
                layoutVerifyUtilityPaymentBinding.billResponse = it
                verifyBillDialog!!.show()
            }
        }
        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    showProgressBar()
                else dismissProgressBar()
            }
        }
        viewModel.result.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                dismissProgressBar()
                when (it) {
                    is ErrorNetworkResponse -> {
                        dialogUtilitiesBinding.responseIcon.setImageResource(R.drawable.ic_error)
                        dialogUtilitiesBinding.tvMessage.text = it.message
                        dialogUtilitiesBinding.tvTitle.text = getString(R.string.error)
                        dialogUtilitiesBinding.tvYes.setOnClickListener {
                            alertDialog?.run {
                                cancel()
                            }
                        }
                    }
                    is SuccessNetworkResponse -> {
                        dialogUtilitiesBinding.responseIcon.setImageResource(R.drawable.animated_check)
                        (dialogUtilitiesBinding.responseIcon.drawable as Animatable).start()
                        dialogUtilitiesBinding.tvMessage.text = it.message
                        dialogUtilitiesBinding.tvTitle.text = getString(R.string.success)
                        val data = it.data
                        if (!data!!.token.isNullOrEmpty()) {
                            dialogUtilitiesBinding.copy.visibility = View.VISIBLE
                            dialogUtilitiesBinding.copy.setOnClickListener {
                                copyTextToClipboard(
                                    requireContext(),
                                    "token",
                                    data.token!!
                                )
                                Toast.makeText(
                                    requireContext(),
                                    "Token copied to clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            copyTextToClipboard(requireContext(), "token", data.token!!)
                        }
                        dialogUtilitiesBinding.tvYes.setOnClickListener {
                            alertDialog?.run {
                                cancel()
                            }
                            requireActivity().onBackPressed()
                        }
                    }
                }
                alertDialog!!.show()
            }
        }
        return when (arguments?.getInt("service_id")) {
            0 -> getCableTvView(inflater, container)
            1 -> getElectricityPageView(inflater, container)
            2 -> getAirtimePageView(inflater, container)
            else -> getInternetPageView(inflater, container)
        }
    }


    private fun showProgressBar() {
        if (progressDialog != null && progressDialog!!.isShowing) return
        progressDialog?.show()
    }

    private fun dismissProgressBar() {
        progressDialog?.run {
            if (isShowing)
                dismiss()
        }
    }

    private fun getElectricityPageView(inflater: LayoutInflater, container: ViewGroup?): View {
        viewModel.setUtilityService("pay_bill")
        viewModel.setUtilityBillType("POWER")
        viewModel.setUtilityServiceType("electricity")
        val powerCategorySpinnerAdapter =
            ServicesSpinnerAdapter(requireActivity().baseContext, listOf("Electricity"))
        val electricityBillers = DataGenerator.electricityBillersList()
        val productsSpinnerAdapter =
            ServicesSpinnerAdapter(
                requireContext(),
                electricityBillers.map { it.billerName },
                electricityBillers.map { it.imageUrl })
        val meterTypeSpinnerAdapter =
            ServicesSpinnerAdapter(requireContext(), listOf("prepaid", "postpaid"))
        binding = LayoutPowerOrElectricityBinding.inflate(inflater, container, false)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.meterType.adapter = meterTypeSpinnerAdapter
        binding.productsSpinner.adapter = productsSpinnerAdapter
        binding.selectACategory.adapter = powerCategorySpinnerAdapter

        var selected = 0
        binding.productsSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selected = position
                    binding.meterType.setSelection(0)
                    viewModel.setUtilityProvider(electricityBillers[selected].prepaidCode)

                }
            }
        binding.meterType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> {
                        viewModel.setUtilityProvider(electricityBillers[selected].prepaidCode)
                        viewModel.setUtilityPackage("Prepaid")
                    }
                    1 -> {
                        viewModel.setUtilityProvider(electricityBillers[selected].postpaidCode)
                        viewModel.setUtilityPackage("Postpaid")
                    }
                }
            }
        }
        return binding.root
    }

    private fun getAirtimePageView(inflater: LayoutInflater, container: ViewGroup?): View {
        viewModel.setUtilityService("vtu")
        viewModel.setUtilityPackage("Airtime")
        viewModel.setUtilityBillType("Airtime")
        airtimeOrDataBinding = LayoutAirtimeOrDataBinding.inflate(inflater, container, false)
        airtimeOrDataBinding.viewmodel = viewModel
        airtimeOrDataBinding.lifecycleOwner = viewLifecycleOwner
        val dataOrAirtimeSpinnerAdapter =
            ServicesSpinnerAdapter(requireContext(), listOf("Airtime", "Mobile Data"))
        val mobileOperatorsBillers = DataGenerator.generateBillers().filter {
            it.service_type == "Airtime"
        }
        val selectNetworkSpinnerAdapter = ServicesSpinnerAdapter(
            requireContext(),
            mobileOperatorsBillers.map { it.biller_code },
            mobileOperatorsBillers.map { it.imageUrl }
        )
        val dataPlanList = DataGenerator.getServiceProviderPlans(context)

        airtimeOrDataBinding.dataOrAirtimeSpinner.adapter = dataOrAirtimeSpinnerAdapter
        airtimeOrDataBinding.selectNetworkSpinner.adapter = selectNetworkSpinnerAdapter
        var selectedNetwork = 0
        airtimeOrDataBinding.selectDataBundleSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.setUtilityPackage("${dataPlanList[selectedNetwork][position].data} - ${dataPlanList[selectedNetwork][position].duration}")
                    airtimeOrDataBinding.priceTextbox.setText(dataPlanList[selectedNetwork][position].price)
                }

            }
        airtimeOrDataBinding.dataOrAirtimeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val visibility = when (position) {
                        0 -> {
                            viewModel.setUtilityPackage("Airtime")
                            viewModel.setUtilityBillType("Airtime")
                            airtimeOrDataBinding.priceTextbox.isEnabled = true
                            airtimeOrDataBinding.currencyTextbox.setBackgroundResource(R.drawable.services_edittext_background)
                            airtimeOrDataBinding.priceTextbox.setBackgroundResource(R.drawable.services_edittext_background)
                            View.GONE
                        }
                        1 -> {
                            airtimeOrDataBinding.priceTextbox.isEnabled = false
                            airtimeOrDataBinding.currencyTextbox.setBackgroundResource(R.drawable.services_edittext_background_disabled)
                            airtimeOrDataBinding.priceTextbox.setBackgroundResource(R.drawable.services_edittext_background_disabled)
                            View.VISIBLE
                        }
                        else -> View.GONE
                    }
                    airtimeOrDataBinding.selectDataBundleSpinner.visibility = visibility
                    airtimeOrDataBinding.selectDataBundleSpinnerIcon.visibility = visibility
                }

            }
        airtimeOrDataBinding.selectNetworkSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedNetwork = position
                    val dataBundleTypeSpinnerAdapter =
                        ServicesSpinnerAdapter(
                            context!!,
                            dataPlanList[position].map { "${it.data} - ${it.duration}" })
                    airtimeOrDataBinding.selectDataBundleSpinner.adapter =
                        dataBundleTypeSpinnerAdapter
                    viewModel.setUtilityProvider(mobileOperatorsBillers[position].biller_code)

                }

            }

        return airtimeOrDataBinding.root
    }

    private fun getInternetPageView(inflater: LayoutInflater, container: ViewGroup?): View {
        viewModel.setUtilityService("pay_bill")
        viewModel.setUtilityBillType("Internet")
        internetSubscriptionBinding =
            LayoutInternetSubscriptionBinding.inflate(inflater, container, false)
        internetSubscriptionBinding.viewmodel = viewModel
        internetSubscriptionBinding.lifecycleOwner = viewLifecycleOwner
        internetSubscriptionBinding.priceTextbox.isEnabled = false
        val internetBiller = DataGenerator.generateBillers().filter {
            it.service_type == "Internet" && it.status == "active"
        }
        val internetProviderSpinnerAdapter =
            ServicesSpinnerAdapter(
                requireContext(),
                internetBiller.map { it.biller_name },
                internetBiller.map { it.imageUrl })

        internetSubscriptionBinding.providerSpinner.adapter = internetProviderSpinnerAdapter
        val billerPlans = DataGenerator.getBillerPlans(requireContext())
        val smileInternetList = billerPlans.smileInternetList
        val spectranetInternetList = billerPlans.spectranetInternetList
        var selected = 0
        internetSubscriptionBinding.providerSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selected = position
                    val packageSpinnerAdapter =
                        when (position) {
                            0 -> {
                                viewModel.setUtilityProvider("SMILE")
                                ServicesSpinnerAdapter(
                                    context!!,
                                    smileInternetList
                                        .map { it.bundleName })
                            }
                            1 -> {
                                viewModel.setUtilityProvider("SPECTRANET")
                                ServicesSpinnerAdapter(
                                    context!!,
                                    spectranetInternetList.map { it.planName })
                            }
                            else -> ServicesSpinnerAdapter(context!!, ArrayList())
                        }
                    internetSubscriptionBinding.packageSpinner.adapter = packageSpinnerAdapter
                }

            }
        internetSubscriptionBinding.packageSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val priceToPay = when (selected) {
                        0 -> {
                            viewModel.setUtilityPackage(smileInternetList[position].bundleName)
                            smileInternetList[position].price.replace("N", "")
                                .replace(",", "")
                        }
                        1 -> {
                            viewModel.setUtilityPackage(spectranetInternetList[position].planName)
                            spectranetInternetList[position].price.replace(",", "")
                        }
                        else -> "0"
                    }
                    internetSubscriptionBinding.priceTextbox.setText(priceToPay)
                }

            }
        return internetSubscriptionBinding.root
    }

    private fun getCableTvView(inflater: LayoutInflater, container: ViewGroup?): View {
        viewModel.setUtilityService("pay_bill")
        viewModel.setUtilityBillType("TV")
        viewModel.setUtilityServiceType("cable_tv")
        cableBinding = LayoutCableTvBinding.inflate(inflater, container, false)
        cableBinding.viewmodel = viewModel
        cableBinding.lifecycleOwner = viewLifecycleOwner
        val cableTvBiller = DataGenerator.generateBillers()
            .filter { it.service_type == "TV" && it.status == "active" }
        val cableTvCategorySpinnerAdapter =
            ServicesSpinnerAdapter(
                requireContext(),
                cableTvBiller.map { it.biller_name },
                cableTvBiller.map { it.imageUrl })

        val plans: Biller.BillerPlans = DataGenerator.getBillerPlans(context)
        val dstvPlans = plans.multichoicePlans.filter { !it.productCode.startsWith("GO") }
        val gotvPlans = plans.multichoicePlans.filter { it.productCode.startsWith("GO") }
        val starTimesPlan = plans.startTimesPlans
        var selectedCategory = 0
        cableBinding.selectCableTvCategory.adapter = cableTvCategorySpinnerAdapter
        cableBinding.selectCableTvCategory.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedCategory = position
                    val tvPackageAdapter = when (position) {
                        0 -> {
                            viewModel.setUtilityProvider("DSTV")
                            ServicesSpinnerAdapter(context!!, dstvPlans.map { it.product })
                        }
                        1 -> {
                            viewModel.setUtilityProvider("GOTV")
                            ServicesSpinnerAdapter(context!!, gotvPlans.map { it.product })
                        }
                        2 -> {
                            viewModel.setUtilityProvider("STARTIMES")
                            ServicesSpinnerAdapter(context!!, starTimesPlan.map { it.plan })
                        }
                        else -> ServicesSpinnerAdapter(context!!, ArrayList())
                    }
                    cableBinding.selectCableTvPackage.adapter = tvPackageAdapter
                }
            }
        cableBinding.selectCableTvPackage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    var feeToPay = when (selectedCategory) {
                        0 -> {
                            viewModel.setUtilityPackage(dstvPlans[position].product)
                            cableBinding.planValidityOptions.visibility = View.GONE
                            dstvPlans[position].newPrice
                        }
                        1 -> {
                            viewModel.setUtilityPackage(gotvPlans[position].product)
                            cableBinding.planValidityOptions.visibility = View.GONE
                            gotvPlans[position].newPrice
                        }
                        2 -> {
                            cableBinding.planValidityOptions.visibility = View.VISIBLE
                            cableBinding.planValidityOptions.check(-1)
                            0
                        }
                        else -> 0
                    }
                    cableBinding.planValidityOptions.setOnCheckedChangeListener { _, checked ->
                        val starTimesPrice = when (checked) {
                            R.id.daily -> {
                                viewModel.setUtilityPackage("${starTimesPlan[position].plan} - Daily")
                                starTimesPlan[position].dailyFee
                            }
                            R.id.weekly -> {
                                viewModel.setUtilityPackage("${starTimesPlan[position].plan} - Weekly")
                                starTimesPlan[position].weeklyFee
                            }
                            R.id.monthly -> {
                                viewModel.setUtilityPackage("${starTimesPlan[position].plan} - Monthly")
                                starTimesPlan[position].monthlyFee
                            }
                            else -> 0
                        }

                        feeToPay = starTimesPrice
                        cableBinding.priceTextbox.setText(feeToPay.toString())
                    }
                    cableBinding.priceTextbox.setText(feeToPay.toString())
                }
            }
        return cableBinding.root
    }
}