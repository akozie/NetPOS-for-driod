package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentReprintBinding
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.util.HISTORY_ACTION_REPRINT

class ReprintFragment : BaseFragment() {
    private lateinit var binding: FragmentReprintBinding
    private lateinit var adapter: ServiceAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReprintBinding.inflate(inflater, container, false)
        adapter = ServiceAdapter {
            if (it.id == 0)
                addFragmentWithoutRemove(TransactionHistoryFragment.NewInstance(action = HISTORY_ACTION_REPRINT))
            else
                printAllDialog()
        }
        return binding.root
    }

    private fun printAllDialog() {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvReprint.layoutManager = GridLayoutManager(context, 2)
        binding.rvReprint.adapter = adapter
        setService()
    }

    private fun setService() {
        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "Reprint One Transaction", R.drawable.ic_print_one))
                add(Service(1, "Reprint All Transactions", R.drawable.ic_print_all))
            }
        adapter.submitList(listOfService)
    }
}