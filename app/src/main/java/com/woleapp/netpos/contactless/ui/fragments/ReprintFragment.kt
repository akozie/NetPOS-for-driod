package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.adapter.ServiceAdapter
import com.woleapp.netpos.contactless.databinding.FragmentReprintBinding
import com.woleapp.netpos.contactless.model.Service
import com.woleapp.netpos.contactless.util.HISTORY_ACTION_REPRINT

class ReprintFragment : BaseFragment() {
    private lateinit var binding: FragmentReprintBinding
    private lateinit var adapter: ServiceAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReprintBinding.inflate(inflater, container, false)
        adapter = ServiceAdapter {
            if (it.id == 0)
                addFragmentWithoutRemove(TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REPRINT))
//            else if (it.id == 1)
//                printAllDialog()
//            else  if (it.id == 2)
//                printAllDialog()
//            else if (it.id == 3)
//                loadCapk()
//            else if (it.id == 4)
//                getThem()

        }
        return binding.root
    }

    private fun getThem() {
//        Timber.e(NetPosSdk.getAids()?.size.toString())
//        Timber.e(NetPosSdk.getCapks()?.size.toString())
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
                //add(Service(2, "Add aid", R.drawable.ic_print_all))
                //add(Service(3, "Add capks", R.drawable.ic_print_all))
                //add(Service(4, "get them", R.drawable.ic_print_all))

            }
        adapter.submitList(listOfService)
    }
}