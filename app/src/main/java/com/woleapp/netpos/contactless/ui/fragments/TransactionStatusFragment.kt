package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woleapp.netpos.contactless.databinding.FragmentTransactionStatusBinding

class TransactionStatusFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentTransactionStatusBinding.inflate(inflater, container, false)
        return binding.root
    }
}