package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentRegisterBinding
import com.woleapp.netpos.contactless.viewmodels.RegistrationViewModel

class RegisterFragment : BaseFragment() {
    private lateinit var binding: FragmentRegisterBinding
    private val viewModel by viewModels<RegistrationViewModel>()
    private lateinit var dialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(layoutInflater).apply {
            executePendingBindings()
            lifecycleOwner = viewLifecycleOwner
            viewmodel = this@RegisterFragment.viewModel
        }
        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.authDone.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    dialog.show()
                }
            }
        }
        dialog = AlertDialog.Builder(requireContext())
            .setTitle("Registration Status")
            .setCancelable(false)
            .setPositiveButton("Continue") { d, _ ->
                d.cancel()
                requireActivity().onBackPressed()
            }
            .setMessage("Business Info Received, our team will contact you in 2 Business Days")
            .create()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSpinner()
    }

    private fun setSpinner() {
        val banks = resources.getStringArray(R.array.bank_codes)
        val adapter = ArrayAdapter.createFromResource(
            requireActivity(), R.array.banks, android.R.layout.simple_spinner_dropdown_item
        )
        binding.spnBank.prompt =
            requireContext().resources.getString(R.string.hint_choose_bank_caps)
        binding.spnBank.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                viewModel.setSelectedBank(banks[p2])
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }
        binding.spnBank.adapter = adapter
    }
}