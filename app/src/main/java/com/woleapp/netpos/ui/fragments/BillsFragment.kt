package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentTransactionsBinding
import com.woleapp.netpos.model.Service

class BillsFragment : BaseFragment() {

    private lateinit var adapter: ServiceAdapter
    private lateinit var binding: FragmentTransactionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        binding.rvTransactionsHeader.text = getString(R.string.bills)
        adapter = ServiceAdapter {
            addFragmentWithoutRemove(UtilitiesPaymentFragment.newInstance(it.id))
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
                add(Service(0, "Cable TV", R.drawable.ic_television))
                add(Service(1, "Power", R.drawable.ic_power))
                add(Service(2, "Airtime & Data", R.drawable.ic_airtime_data))
                add(Service(3, "Internet", R.drawable.ic_globe))
            }
        adapter.submitList(listOfService)
    }
}
