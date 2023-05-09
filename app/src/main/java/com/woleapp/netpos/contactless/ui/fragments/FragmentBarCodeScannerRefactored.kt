package com.woleapp.netpos.contactless.ui.fragments

// ktlint-disable no-wildcard-imports
// ktlint-disable no-wildcard-imports
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.gson.Gson
import com.woleapp.netpos.contactless.R
import com.woleapp.netpos.contactless.databinding.FragmentBarCodeScannerRefactoredBinding
import com.woleapp.netpos.contactless.domain.PermissionHandler
import com.woleapp.netpos.contactless.model.QrScannedDataModel
import com.woleapp.netpos.contactless.usecases.PermissionHandlerImpl
import com.woleapp.netpos.contactless.util.AppConstants.PERMISSION_REQUEST_CODE
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_READ_RESULT_BUNDLE_KEY
import com.woleapp.netpos.contactless.util.AppConstants.STRING_QR_READ_RESULT_REQUEST_KEY
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.base64ToPlainText
import com.woleapp.netpos.contactless.util.RandomPurposeUtil.showSnackBar
import com.woleapp.netpos.contactless.util.vibrateThePhone
import com.woleapp.netpos.contactless.viewmodels.ScanQrViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class FragmentBarCodeScannerRefactored :
    Fragment(),
    PermissionHandler by PermissionHandlerImpl() {
    private lateinit var binding: FragmentBarCodeScannerRefactoredBinding
    private lateinit var detector: BarcodeDetector
    private lateinit var cameraSource: CameraSource
    private lateinit var surfaceView: SurfaceView

    @Inject
    lateinit var gson: Gson
    private val viewModel by activityViewModels<ScanQrViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_bar_code_scanner_refactored, container, false)
        surfaceView = binding.scannerView
        detector = BarcodeDetector.Builder(requireContext())
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        cameraSource = CameraSource.Builder(requireContext(), detector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true)
            .build()

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showSnackBar(requireView(), getString(R.string.scanning_not_completed))
                    requireActivity().supportFragmentManager.popBackStack()
                }
            },
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        permissionHandler(
            requireActivity(),
            requireContext(),
            Manifest.permission.CAMERA,
            PERMISSION_REQUEST_CODE,
            R.string.camera_perm_rationale,
        ) {
            startCameraGoogleVision()
        }
    }

    private fun startCameraGoogleVision() {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    cameraSource.start(p0)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    cameraSource.start(p0)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraSource.stop()
            }
        })

        detector.setProcessor(object : Detector.Processor<Barcode?> {
            override fun release() {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.scanner_closed),
                    Toast.LENGTH_SHORT,
                )
                    .show()
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode?>) {
                val barcodes: SparseArray<Barcode?> = detections.detectedItems
                if (barcodes.size() > 0) {
                    val qrCodeText = barcodes.valueAt(0)?.rawValue
                    activity?.runOnUiThread {
                        cameraSource.stop()
                        qrCodeText?.let {
                            vibrateThePhone(requireContext())
                            val scannedData: QrScannedDataModel? = try {
                                gson.fromJson(base64ToPlainText(it), QrScannedDataModel::class.java)
                            } catch (e: Exception) {
                                null
                            }
                            if (scannedData != null) {
                                viewModel.setCardScheme(scannedData.card_scheme) // Save account type for future ref
                                Toast.makeText(
                                    activity,
                                    getText(R.string.qr_scanned),
                                    Toast.LENGTH_LONG,
                                )
                                    .show()
                                requireActivity().supportFragmentManager.setFragmentResult(
                                    STRING_QR_READ_RESULT_REQUEST_KEY,
                                    bundleOf(STRING_QR_READ_RESULT_BUNDLE_KEY to scannedData),
                                )
                            } else {
                                showSnackBar(binding.root, getString(R.string.invalid_qr))
                            }
                            parentFragmentManager.popBackStack()
                        }
                    }
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        cameraSource.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        this.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(requireContext(), getString(R.string.perm_granted), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(requireContext(), getString(R.string.perm_denied), Toast.LENGTH_SHORT)
            .show()
    }
}
