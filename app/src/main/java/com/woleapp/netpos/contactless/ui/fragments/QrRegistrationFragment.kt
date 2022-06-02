package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.adapter.ZenithQrMCCAdapter
import com.woleapp.netpos.contactless.databinding.DialogFragmentZenithMccBinding
import com.woleapp.netpos.contactless.databinding.FragmentCreateZenithMerchantBinding
import com.woleapp.netpos.contactless.model.LoadingState
import com.woleapp.netpos.contactless.model.ZenithMCCDto
import com.woleapp.netpos.contactless.viewmodels.QRViewModel

import timber.log.Timber

class QrRegistrationFragment : BaseFragment() {

    private val viewModel by viewModels<QRViewModel>()
    private lateinit var mccAlertDialog: AlertDialog
    private lateinit var mccDialogBinding: DialogFragmentZenithMccBinding
    private lateinit var adapter: ZenithQrMCCAdapter

    companion object {
        fun newInstance() = QrRegistrationFragment()
    }

    private lateinit var binding: FragmentCreateZenithMerchantBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateZenithMerchantBinding.inflate(inflater, container, false)
            .apply {
                lifecycleOwner = viewLifecycleOwner
                executePendingBindings()
                this.viewmodel = this@QrRegistrationFragment.viewModel
            }
        mccDialogBinding =
            DialogFragmentZenithMccBinding.inflate(inflater, container, false).apply {
                lifecycleOwner = viewLifecycleOwner
                executePendingBindings()
            }
        adapter = ZenithQrMCCAdapter {
            mccAlertDialog.cancel()
            binding.getMCC.text = it.merchantCategoryDescription
            viewModel.setSelectedMerchantCategory(it)
        }
        mccDialogBinding.mccList.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        mccDialogBinding.mccList.adapter = adapter
        val states = requireContext().resources.getStringArray(R.array.ng_states)
        binding.statesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.clearSelectedCity()
                binding.citySpinner.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    ArrayList<String>()
                )
                viewModel.getCities(state = states[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        binding.citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setSelectedCity(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        viewModel.zenithCityList.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                val cityAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    it.map { city -> city.cityName })
                binding.citySpinner.adapter = cityAdapter
            }
        }
        val statesAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, states)
        binding.statesSpinner.adapter = statesAdapter
        mccAlertDialog = AlertDialog.Builder(
            requireContext(),
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
        ).setView(mccDialogBinding.root)
            .create()
        binding.getMCC.setOnClickListener {
            mccAlertDialog.show()
            viewModel.getZenithMCC(ZenithMCCDto())
        }
        viewModel.loadingStateLiveData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                when (it.loadingState) {
                    LoadingState.LOADING_COMPLETE -> {
                        mccDialogBinding.loadingMore.visibility = View.GONE
                        mccDialogBinding.progressHorizontal.visibility = View.GONE
                    }
                    LoadingState.LOADING_FAILED -> {
                        Toast.makeText(
                            requireContext(),
                            "An error occurred while fetching",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    LoadingState.LOADING_MORE -> mccDialogBinding.loadingMore.visibility =
                        View.VISIBLE
                    LoadingState.LOADING_INITIAL -> mccDialogBinding.progressHorizontal.visibility =
                        View.VISIBLE
                }
                Timber.e(it.loadingState.name)
            }
        }
        viewModel.zenithMccList.observe(viewLifecycleOwner) {
            Timber.e("list in fragment: ${it.size}")
            adapter.submitList(it)
        }
        viewModel.initSearchFilter()
        mccDialogBinding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.textChangeComplete()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    viewModel.textChanged(filter = it)
                }
                return false
            }

        })
        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.zenithQrRegistrationDone.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    requireActivity().onBackPressed()
            }
        }
        return binding.root
    }
}