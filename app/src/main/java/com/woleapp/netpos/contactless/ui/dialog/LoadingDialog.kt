package com.woleapp.netpos.contactless.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.LayoutLoadingDialogBinding
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadingDialog @Inject constructor() : DialogFragment() {
    private lateinit var binding: LayoutLoadingDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.layout_loading_dialog, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setBackgroundDrawableResource(R.drawable.curve_bg)
            isCancelable = false
        }
    }
}
