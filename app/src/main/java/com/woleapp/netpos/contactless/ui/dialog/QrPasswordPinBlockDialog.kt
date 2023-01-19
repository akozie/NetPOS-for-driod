package com.woleapp.netpos.contactless.ui.dialog // ktlint-disable filename

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.* // ktlint-disable no-wildcard-imports
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.LayoutRandomizedPinBlockBinding
import com.woleapp.netpos.contactless.util.AppConstants.PIN_BLOCK_BK
import com.woleapp.netpos.contactless.util.AppConstants.PIN_BLOCK_RK
import timber.log.Timber
import java.util.*

class QrPasswordPinBlockDialog : DialogFragment() {
    private var integerList = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private lateinit var binding: LayoutRandomizedPinBlockBinding
    private lateinit var tvMessageTv: TextView
    private lateinit var etPinEt: EditText
    private lateinit var btnConfirmBtn: Button
    private lateinit var btnClearIv: ImageView
    private lateinit var btnEscIv: TextView
    private lateinit var btn0Tv: TextView
    private lateinit var btn1Tv: TextView
    private lateinit var btn2Tv: TextView
    private lateinit var btn3Tv: TextView
    private lateinit var btn4Tv: TextView
    private lateinit var btn5Tv: TextView
    private lateinit var btn6Tv: TextView
    private lateinit var btn7Tv: TextView
    private lateinit var btn8Tv: TextView
    private lateinit var btn9Tv: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.layout_randomized_pin_block,
            container,
            false
        )

        initViews()

        val builder = StringBuilder()
        val buttons =
            arrayOf(btn0Tv, btn1Tv, btn2Tv, btn3Tv, btn4Tv, btn5Tv, btn6Tv, btn7Tv, btn8Tv, btn9Tv)
        for (i in buttons.indices) {
            buttons[i].setOnClickListener { v: View ->
                if (etPinEt.text.toString().length >= 4) {
                    tvMessageTv.text = getString(R.string.max_input)
                    return@setOnClickListener
                }
                builder.append((v as TextView).text.toString())
                etPinEt.setText(builder.toString())
            }
        }
        btnEscIv.setOnClickListener {
            dialog?.cancel()
            dialog?.dismiss()
            dismiss()
        }
        btnClearIv.setOnClickListener {
            if (builder.isNotEmpty()) {
                builder.deleteCharAt(builder.length - 1)
                etPinEt.setText(builder.toString())
            }
        }
        btnConfirmBtn.setOnClickListener {
            if (etPinEt.text.toString().length < 4) {
                Toast.makeText(context, getString(R.string.pin_too_short), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            setFragmentResult(PIN_BLOCK_RK, bundleOf(PIN_BLOCK_BK to etPinEt.text.toString()))
            dialog?.cancel()
            dialog?.dismiss()
            dismiss()
        }

        return binding.root
    }

    override fun getTheme(): Int {
        return R.style.DialogTheme
    }

    override fun onResume() {
        super.onResume()
        etPinEt.text.clear()
        Collections.shuffle(integerList)
        Timber.d("SHUFFLED_LIST_OF_INT====>$integerList")
        setNumberOnTextViews(integerList)
    }

    private fun initViews() {
        with(binding) {
            tvMessageTv = tvMessage
            etPinEt = etPin
            btnConfirmBtn = btnConfirm
            btnClearIv = btnClear
            btnEscIv = btnEsc
            btn0Tv = btn0
            btn1Tv = btn1
            btn2Tv = btn2
            btn3Tv = btn3
            btn4Tv = btn4
            btn5Tv = btn5
            btn6Tv = btn6
            btn7Tv = btn7
            btn8Tv = btn8
            btn9Tv = btn9
        }
    }

    private fun setNumberOnTextViews(integerList: List<String>) {
        btn0Tv.text = integerList[0]
        btn1Tv.text = integerList[1]
        btn2Tv.text = integerList[2]
        btn3Tv.text = integerList[3]
        btn4Tv.text = integerList[4]
        btn5Tv.text = integerList[5]
        btn6Tv.text = integerList[6]
        btn7Tv.text = integerList[7]
        btn8Tv.text = integerList[8]
        btn9Tv.text = integerList[9]
    }
}
