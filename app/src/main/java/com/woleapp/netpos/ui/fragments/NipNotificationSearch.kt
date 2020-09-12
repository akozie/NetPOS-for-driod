package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.databinding.FragmentNipNotificationSearchBinding
import com.woleapp.netpos.model.User
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.PREF_USER_TOKEN
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class NipNotificationSearch : BaseFragment() {
    private lateinit var binding: FragmentNipNotificationSearchBinding
    val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNipNotificationSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.nip.root.visibility = View.GONE
        val userToken = Prefs.getString(PREF_USER_TOKEN, "")
        val user = gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        super.onViewCreated(view, savedInstanceState)
        binding.searchButton.setOnClickListener {
            if (binding.sessionCode.text.toString().isEmpty())
                return@setOnClickListener
            StormApiClient.getInstance().getNotificationByReference(
                binding.sessionCode.text.toString(),
                user.netplus_id!!, userToken
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { binding.progressCircular.visibility = View.VISIBLE }
                .doFinally { binding.progressCircular.visibility = View.GONE }
                .subscribe { notification, throwable ->
                    notification?.let {
                        binding.nip.nip = it
                        binding.nip.root.visibility = View.VISIBLE
                    }
                    throwable?.let {
                        Timber.e("Nip Error: ${it.localizedMessage}")
                        Toast.makeText(requireContext(), "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}