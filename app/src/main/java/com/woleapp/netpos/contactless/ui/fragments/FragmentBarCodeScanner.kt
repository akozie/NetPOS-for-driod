package com.woleapp.netpos.contactless.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.budiyev.android.codescanner.* // ktlint-disable no-wildcard-imports
import com.google.gson.Gson
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.model.QrScannedDataModel
import com.woleapp.netpos.contactless.viewmodels.ScanQrViewModel
import com.woleapp.netpos.contactless.util.AppConstants.PERMISSION_REQUEST_CODE
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_READ_RESULT_BUNDLE_KEY
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_READ_RESULT_REQUEST_KEY
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.base64ToPlainText
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.showSnackBar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FragmentBarCodeScanner @Inject constructor() : Fragment() {
    @Inject
    lateinit var gson: Gson
    private lateinit var codeScanner: CodeScanner
    private val viewModel by activityViewModels<ScanQrViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showSnackBar(requireView(), getString(R.string.scanning_not_completed))
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        )

        return inflater.inflate(R.layout.layout_fragment_barcode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val scannerView = view.findViewById<CodeScannerView>(R.id.scanner_view)
        codeScanner = CodeScanner(requireActivity(), scannerView)

        if (checkPermission()) {
            codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
            codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
            // ex. listOf(BarcodeFormat.QR_CODE)
            codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
            codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
            codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
            codeScanner.isFlashEnabled = false
            codeScanner.decodeCallback = DecodeCallback {
                activity?.runOnUiThread {
                    // Pass the data to transaction fragment via fragment result api
                    Timber.d("READ_DATA=====>%s", it.text)
                    val scannedData: QrScannedDataModel? = try {
                        gson.fromJson(base64ToPlainText(it.text), QrScannedDataModel::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    if (scannedData != null) {
                        viewModel.setCardScheme(scannedData.card_scheme) // Save account type for future ref
                        Toast.makeText(activity, getText(R.string.qr_scanned), Toast.LENGTH_LONG)
                            .show()
                        requireActivity().supportFragmentManager.setFragmentResult(
                            STRING_QR_READ_RESULT_REQUEST_KEY,
                            bundleOf(STRING_QR_READ_RESULT_BUNDLE_KEY to scannedData)
                        )
                    } else {
                        showSnackBar(view, getString(R.string.invalid_qr))
                    }
                    parentFragmentManager.popBackStack()
                }
            }
            scannerView.setOnClickListener {
                codeScanner.startPreview()
            }
        } else {
            requestPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) === PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf<String>(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    requireContext(),
                    "Permission Granted",
                    Toast.LENGTH_SHORT
                ).show()

                // main logic
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission Denied",
                    Toast.LENGTH_SHORT
                ).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA
                        )
                        !== PackageManager.PERMISSION_GRANTED
                    ) {
                        showMessageOKCancel() { _, _ ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermission()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showMessageOKCancel(okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(requireActivity())
            .setMessage("You need to allow access permissions")
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}
