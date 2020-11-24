package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.entities.TransactionType
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentTransactionsBinding
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.util.HISTORY_ACTION_REFUND

class TransactionsFragment : BaseFragment() {

    private lateinit var adapter: ServiceAdapter
    private lateinit var binding: FragmentTransactionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        adapter = ServiceAdapter {
            val nextFrag : Fragment = when (it.id) {
                0 -> SalesFragment.newInstance()
                1 -> TransactionHistoryFragment.NewInstance(action = HISTORY_ACTION_REFUND)
                3 -> ReprintFragment()
                else -> SalesFragment.newInstance(TransactionType.CASH_ADVANCE)
            }
            addFragmentWithoutRemove(nextFrag)
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
                add(Service(0, "Sales", R.drawable.ic_purchase))
                add(Service(1, "Refund", R.drawable.ic_loop))
                add(Service(2, "Cash Advance", R.drawable.ic_pay_cash_icon))
                add(Service(3, "Reprint", R.drawable.ic_print))
            }
        adapter.submitList(listOfService)
    }
}