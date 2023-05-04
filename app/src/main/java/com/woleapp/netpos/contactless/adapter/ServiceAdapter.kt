package com.woleapp.netpos.contactless.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woleapp.netpos.contactless.databinding.LayoutServiceItemBinding
import com.woleapp.netpos.contactless.model.Service

typealias ServiceClickListener = (Service) -> Unit

private object ItemDiffUtil : DiffUtil.ItemCallback<Service>() {
    override fun areItemsTheSame(oldItem: Service, newItem: Service): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Service, newItem: Service): Boolean =
        oldItem.id == newItem.id
}

class ServiceAdapter(private val serviceClickListener: ServiceClickListener) :
    ListAdapter<Service, ServiceViewHolder>(
        ItemDiffUtil,
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder =
        ServiceViewHolder.from(
            parent,
            serviceClickListener,
        )

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) =
        holder.bind(getItem(position))
}

class ServiceViewHolder private constructor(
    private val binding: LayoutServiceItemBinding,
    private val serviceClickListener: ServiceClickListener,
) :
    RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun from(parent: ViewGroup, serviceClickListener: ServiceClickListener) =
            ServiceViewHolder(
                LayoutServiceItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
                serviceClickListener,
            )
    }

    fun bind(service: Service) {
        binding.serviceIconCard.setOnClickListener { serviceClickListener.invoke(service) }
        binding.executePendingBindings()
        binding.service.text = service.serviceName
        binding.serviceIcon.setImageResource(service.serviceImage)
    }
}
