package com.example.upipaymenttracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object TransactionExporter {

    fun generateCsv(transactions: List<TransactionModel>): String {
        val builder = StringBuilder()
        builder.append("Date,Category,Merchant/Note,Amount\n")
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        transactions.forEach {
            val date = sdf.format(Date(it.timestamp))
            builder.append("$date,${it.category},\"${it.note}\",${it.amount}\n")
        }
        return builder.toString()
    }

    fun generateTextReport(transactions: List<TransactionModel>, title: String): String {
        val builder = StringBuilder()
        builder.append("=== $title ===\n\n")
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        transactions.forEach {
            val date = sdf.format(Date(it.timestamp))
            builder.append("[$date] ${it.category}: ₹${it.amount}\n")
            if (it.note.isNotEmpty()) builder.append("Note: ${it.note}\n")
            builder.append("-" .repeat(20) + "\n")
        }
        return builder.toString()
    }

    fun generatePdf(context: Context, transactions: List<TransactionModel>, title: String, uri: Uri) {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 12f
        }

        var pageNumber = 1
        var myPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var myPage = pdfDocument.startPage(myPageInfo)
        var canvas: Canvas = myPage.canvas

        canvas.drawText(title, 40f, 50f, titlePaint)
        var y = 100f
        val sdf = SimpleDateFormat("dd-MM-yy HH:mm", Locale.getDefault())

        transactions.forEach { trans ->
            if (y > 800f) {
                pdfDocument.finishPage(myPage)
                pageNumber++
                myPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                myPage = pdfDocument.startPage(myPageInfo)
                canvas = myPage.canvas
                y = 50f
            }

            val date = sdf.format(Date(trans.timestamp))
            canvas.drawText("$date  |  ${trans.category}  |  ₹${trans.amount}", 40f, y, textPaint)
            y += 20f
            if (trans.note.isNotEmpty()) {
                canvas.drawText("  Note: ${trans.note}", 40f, y, textPaint)
                y += 20f
            }
            y += 10f
        }

        pdfDocument.finishPage(myPage)
        
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

    fun writeToUri(context: Context, uri: Uri, content: String) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateSyncCode(transactions: List<TransactionModel>): String {
        val sharedData = transactions.filter { it.isShared }
            .joinToString("|") { "${it.amount};${it.category};${it.note};${it.timestamp};${it.splitAmount}" }
        return android.util.Base64.encodeToString(sharedData.toByteArray(), android.util.Base64.NO_WRAP)
    }

    fun decodeSyncCode(code: String): List<TransactionModel> {
        return try {
            val decodedBytes = android.util.Base64.decode(code, android.util.Base64.NO_WRAP)
            val data = String(decodedBytes)
            if (data.isEmpty()) return emptyList()
            data.split("|").map { 
                val parts = it.split(";")
                TransactionModel(
                    amount = parts[0],
                    category = parts[1],
                    note = parts[2],
                    timestamp = parts[3].toLong(),
                    isShared = true,
                    splitAmount = parts[4].toDoubleOrNull()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}