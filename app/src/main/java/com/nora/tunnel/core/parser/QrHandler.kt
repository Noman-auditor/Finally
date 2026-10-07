package com.nora.tunnel.core.parser

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

object QrHandler {
    fun generate(text: String): Bitmap = BarcodeEncoder().encodeBitmap(text, BarcodeFormat.QR_CODE, 512, 512)
    fun parseQrContent(text: String) = ConfigParser.parse(text) // Reuse same Parse->Validate->Preview->Save flow
}
