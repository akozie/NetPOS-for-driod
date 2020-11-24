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
import com.woleapp.netpos.databinding.FragmentNipNotificationListBinding
import com.woleapp.netpos.databinding.LayoutNipNotificationItemBinding
import com.woleapp.netpos.model.NipNotification
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.util.print
import com.woleapp.netpos.util.printAllNotifications
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class NipNotificationListFragment : BaseFragment() {

    companion object {
        const val LAST_TWO = 1
        const val END_OF_DAY = 2
        private const val NOTIFICATION_TYPE = "notification_type"
        fun newInstance(notificationType: Int): NipNotificationListFragment =
            NipNotificationListFragment().apply {
                arguments = Bundle().apply {
                    putInt(NOTIFICATION_TYPE, notificationType)
                }
            }
    }

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
        adapter = NipAdapter {
            it.print().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _, _ -> }
        }
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
        binding.printAll.setOnClickListener {
            adapter.currentList.let {
                if (it.isEmpty().not()) {
                    it.printAllNotifications()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({

                        }, { throwable ->
                            Timber.e(throwable.localizedMessage)
                        }, {

                        })
                }
            }
        }
    }

    private fun getNotifications() {
        if (NetPosTerminalConfig.getTerminalId().isEmpty()) {
            binding.refresh.isRefreshing = false
            Toast.makeText(requireContext(), "No terminal Id", Toast.LENGTH_SHORT).show()
            return
        }
        val nipService = StormApiClient.getNipInstance()
        val request = when (requireArguments().getInt(NOTIFICATION_TYPE, LAST_TWO)) {
            END_OF_DAY -> nipService.getEndOfDayNotifications(
                NetPosTerminalConfig.getTerminalId(),
                "",
                ""
            )
            else -> nipService.getLastTwoTransfers("2214160A")
        }
        request
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { binding.refresh.isRefreshing = false }
            .subscribe { notifications, throwable ->
                notifications?.let {
                    adapter.submitList(it)
                }
                throwable?.let {
                    Timber.e(it.localizedMessage)
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

typealias NipNotificationItemClickListener = (NipNotification) -> Unit

class NipAdapter(private val nipNotificationItemClickListener: NipNotificationItemClickListener) :
    ListAdapter<NipNotification, NipViewHolder>(NipItemCallBack) {
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
        holder.binding.print.setOnClickListener {
            nipNotificationItemClickListener.invoke(getItem(position))
        }
        holder.bind(getItem(position))
    }

}

class NipViewHolder(val binding: LayoutNipNotificationItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(newItem: NipNotification) {
        binding.apply {
            executePendingBindings()
            nip = newItem
        }
    }

}