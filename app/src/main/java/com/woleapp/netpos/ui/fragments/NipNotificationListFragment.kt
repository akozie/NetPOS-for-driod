package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.databinding.FragmentNipNotificationListBinding
import com.woleapp.netpos.databinding.LayoutNipNotificationItemBinding
import com.woleapp.netpos.model.NipNotification
import com.woleapp.netpos.model.User
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.PREF_USER_TOKEN
import com.woleapp.netpos.util.disposeWith
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class NipNotificationListFragment : BaseFragment() {
    private lateinit var binding: FragmentNipNotificationListBinding
    private lateinit var adapter: NipAdapter
    private val compositeDisposable = CompositeDisposable()
    val gson = Gson()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNipNotificationListBinding.inflate(inflater, container, false)
        adapter = NipAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.notificationList.adapter = adapter
        binding.refresh.isRefreshing = true
        binding.refresh.setOnRefreshListener { getNotifications() }
        binding.notificationList.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        getNotifications()
    }

    private fun getNotifications() {
        val user = gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        val userToken = Prefs.getString(PREF_USER_TOKEN, "")
        val list = listOf(
            NipNotification("0084270572", "Aki Paw paw", "0812467747", "100", "23-1930"),
            NipNotification("0084270572", "Aki Paw paw", "0812467747", "100", "23-1930"),
            NipNotification("0084270572", "Aki Paw paw", "0812467747", "100", "23-1930")
        )
        adapter.submitList(list)
        if (user.terminal_id.isNullOrEmpty()) {
            binding.refresh.isRefreshing = false
            Toast.makeText(requireContext(), "No terminal Id", Toast.LENGTH_SHORT).show()
            return
        }
        StormApiClient.getInstance().getNotifications(
            user.terminal_id!!,
            accessCode = userToken,
            clientId = user.netplus_id!!
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { binding.refresh.isRefreshing = false }
            .subscribe { notifications, throwable ->
                notifications?.let {
                    adapter.submitList(it)
                }
                throwable.let {
                    Toast.makeText(
                        requireContext(),
                        "Message: ${it.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .disposeWith(compositeDisposable)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}

object NipItemCallBack : DiffUtil.ItemCallback<NipNotification>() {
    override fun areItemsTheSame(oldItem: NipNotification, newItem: NipNotification): Boolean {
        val isDateSame = oldItem.createdAt == newItem.createdAt
        val isAmountSame = oldItem.amount == newItem.amount
        val isSourceSame = oldItem.sourceAccountNumber == newItem.sourceAccountNumber
        return isAmountSame && isDateSame && isSourceSame
    }

    override fun areContentsTheSame(oldItem: NipNotification, newItem: NipNotification): Boolean {
        val isDateSame = oldItem.createdAt == newItem.createdAt
        val isAmountSame = oldItem.amount == newItem.amount
        val isSourceSame = oldItem.sourceAccountNumber == newItem.sourceAccountNumber
        return isAmountSame && isDateSame && isSourceSame
    }

}

class NipAdapter : ListAdapter<NipNotification, NipViewHolder>(NipItemCallBack) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NipViewHolder {
        return NipViewHolder(
            LayoutNipNotificationItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: NipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class NipViewHolder(private val binding: LayoutNipNotificationItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(newItem: NipNotification) {
        binding.apply {
            executePendingBindings()
            nip = newItem
        }
    }

}