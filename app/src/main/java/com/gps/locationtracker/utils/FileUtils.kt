package com.gps.locationtracker.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.gps.locationtracker.data.models.LocationData
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    fun generateLogPdf(context: Context, locations: List<LocationData>): File? {
        if (locations.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 12f
        }

        var pageNumber = 1
        var y = 50f
        val margin = 40f
        val lineSpacing = 20f

        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Title
        canvas.drawText("${Constants.APP_NAME} - Location Logs", margin, y, titlePaint)
        y += lineSpacing * 2

        locations.forEach { location ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }

            canvas.drawText("Time: ${location.formattedTime} | Source: ${location.source}", margin, y, textPaint)
            y += lineSpacing
            canvas.drawText("Lat: ${location.latitude} | Lon: ${location.longitude}", margin, y, textPaint)
            y += lineSpacing
            canvas.drawText("Acc: ${location.accuracy}m | Alt: ${location.altitude}m", margin, y, textPaint)
            y += lineSpacing * 1.5f
            
            // Draw a separator line
            paint.color = Color.LTGRAY
            canvas.drawLine(margin, y - 5f, 555f, y - 5f, paint)
            y += lineSpacing / 2
        }

        pdfDocument.finishPage(page)

        // Create filename
        // Format: Time of export(hhmmss) + 1st log date and time + to + last log date and time + App name
        val exportTime = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        val firstLog = locations.last().formattedTime.replace(" ", "_").replace(":", "")
        val lastLog = locations.first().formattedTime.replace(" ", "_").replace(":", "")
        val appName = Constants.APP_NAME.replace(" ", "")
        
        val fileName = "${exportTime}_${firstLog}_to_${lastLog}_${appName}.pdf"
        
        // Save to app internal cache first for sharing/uploading
        val file = File(context.cacheDir, fileName)
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            Timber.e("Error writing PDF: ${e.message}")
            pdfDocument.close()
            null
        }
    }

    fun saveFileToDownloads(context: Context, file: File): Boolean {
        val appName = Constants.APP_NAME.replace(" ", "")
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), appName)
        
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val destFile = File(downloadsDir, file.name)
        return try {
            file.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Timber.e("Error saving to downloads: ${e.message}")
            false
        }
    }

    fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Logs"))
    }
}
