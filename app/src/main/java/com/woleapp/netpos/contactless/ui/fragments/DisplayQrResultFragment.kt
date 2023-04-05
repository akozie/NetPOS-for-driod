package com.woleapp.netpos.contactless.ui.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentDisplayQrResultBinding
import com.woleapp.netpos.contactless.util.AppConstants
import com.woleapp.netpos.contactless.util.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplayQrResultFragment : Fragment() {

    private lateinit var binding: FragmentDisplayQrResultBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_display_qr_result, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val displayQrResult =
            Prefs.getString(AppConstants.PAYMENT_WITH_QR_STRING, "")

        val imageBytes = Base64.decode(displayQrResult, Base64.DEFAULT)
        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        if (displayQrResult.isNotEmpty()) {
            Glide.with(requireContext()).load(decodedImage).into(binding.paymentQr)
        }
    }
}