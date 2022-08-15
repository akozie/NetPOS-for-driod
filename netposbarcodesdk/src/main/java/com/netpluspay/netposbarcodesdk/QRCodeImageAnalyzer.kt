package com.netpluspay.netposbarcodesdk

import android.graphics.ImageFormat.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader


internal class QRCodeImageAnalyzer(private val qrCodeFoundListener: QRCodeFoundListener) :
    ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        if (image.format == YUV_420_888 || image.format == YUV_422_888 || image.format == YUV_444_888) {
            val byteBuffer = image.planes.first().buffer
            val imageData = ByteArray(byteBuffer.capacity())
            byteBuffer.get(imageData)
            val source = PlanarYUVLuminanceSource(
                imageData,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                val result: Result = QRCodeMultiReader().decode(binaryBitmap)
                qrCodeFoundListener.onQrCodeFound(result.text)
            } catch (e: FormatException) {
                qrCodeFoundListener.onQrCodeNotFound()
            } catch (e: ChecksumException) {
                qrCodeFoundListener.onQrCodeNotFound()
            } catch (e: NotFoundException) {
                qrCodeFoundListener.onQrCodeNotFound()
            }
        }
        image.close()
    }
}