package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager

import com.netpluspay.netpossdk.NetPosSdk
import com.pos.sdk.emvcore.PosEmvCapk
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentReprintBinding
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.HISTORY_ACTION_REPRINT
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import timber.log.Timber.e

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
        Timber.e(NetPosSdk.getAids()?.size.toString())
        Timber.e(NetPosSdk.getCapks()?.size.toString())
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