package com.woleapp.netpos.contactless.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.danbamitale.epmslib.entities.TransactionResponse

class EODAdapter(val listener: TransactionClickListener) :
    ListAdapter<TransactionResponse, TransactionsViewHolder>(TransactionItemDiffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TransactionsViewHolder.from(parent)

    override fun onBindViewHolder(holder: TransactionsViewHolder, position: Int) {
        getItem(position)?.let { transactionResponse ->
            holder.binding.root.setOnClickListener { listener.invoke(transactionResponse) }
            holder.bind(transactionResponse)
        }
    }
}