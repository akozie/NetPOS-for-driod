package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentExisitingCustomersRegistrationBinding


class ExistingCustomersRegistrationFragment : Fragment() {

    private lateinit var binding : FragmentExisitingCustomersRegistrationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,R.layout.fragment_exisiting_customers_registration,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}