package com.woleapp.netpos.contactless.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woleapp.netpos.contactless.databinding.ItemMccBinding
import com.woleapp.netpos.contactless.model.ZenithMerchantCategory

typealias ZenithMCCClickListener = (item: ZenithMerchantCategory) -> Unit

object ZenithMCCDiffUtil : DiffUtil.ItemCallback<ZenithMerchantCategory>() {
    override fun areItemsTheSame(
        oldItem: ZenithMerchantCategory,
        newItem: ZenithMerchantCategory
    ): Boolean = oldItem.merchantCategoryCode == newItem.merchantCategoryCode

    override fun areContentsTheSame(
        oldItem: ZenithMerchantCategory,
        newItem: ZenithMerchantCategory
    ): Boolean = oldItem.merchantCategoryCode == newItem.merchantCategoryCode

}

class ZenithQrMCCAdapter(private val clickListener: ZenithMCCClickListener) :

    PagedListAdapter<ZenithMerchantCategory, ZenithQrMCCViewHolder>(ZenithMCCDiffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZenithQrMCCViewHolder {
        return ZenithQrMCCViewHolder.from(viewGroup = parent)
    }

    override fun onBindViewHolder(holder: ZenithQrMCCViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
            holder.binding.root.setOnClickListener { _ -> clickListener.invoke(it) }
        }
    }

}

class ZenithQrMCCViewHolder private constructor(val binding: ItemMccBinding) :
    RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun from(viewGroup: ViewGroup): ZenithQrMCCViewHolder {
            return ZenithQrMCCViewHolder(
                ItemMccBinding.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewGroup,
                    false
                )
            )
        }
    }

    fun bind(zenithMerchantCategory: ZenithMerchantCategory) {
        binding.text.text = zenithMerchantCategory.merchantCategoryDescription
    }
}