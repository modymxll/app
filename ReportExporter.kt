package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ReportExporter {

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun shareExcelCsv(
        context: Context,
        fileNamePrefix: String,
        headers: List<String>,
        rows: List<List<String>>,
        chooserTitle: String
    ): Boolean {
        return runCatching {
            val file = File(exportsDir(context), "${fileNamePrefix}_${System.currentTimeMillis()}.csv")
            val sb = StringBuilder()
            // UTF-8 BOM so Microsoft Excel renders Arabic cleanly
            sb.append('\uFEFF')
            sb.append(headers.joinToString(",") { csvEscape(it) }).append("\r\n")
            rows.forEach { row ->
                sb.append(row.joinToString(",") { csvEscape(it) }).append("\r\n")
            }
            file.writeText(sb.toString(), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
            true
        }.getOrDefault(false)
    }

    fun sharePdfReport(
        context: Context,
        fileNamePrefix: String,
        title: String,
        subtitle: String,
        lines: List<String>,
        chooserTitle: String
    ): Boolean {
        return runCatching {
            val file = File(exportsDir(context), "${fileNamePrefix}_${System.currentTimeMillis()}.pdf")
            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val headerPaint = Paint().apply {
                color = Color.parseColor("#1BBDD2")
                style = Paint.Style.FILL
            }
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 18f
                isFakeBoldText = true
            }
            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#E6F8FB")
                textSize = 11f
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#142428")
                textSize = 10.5f
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E0ECEF")
                strokeWidth = 1f
            }

            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas

            fun drawHeader() {
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 76f, headerPaint)
                canvas.drawText("BUBBLES LAUNDRY — $title", 28f, 34f, titlePaint)
                canvas.drawText(subtitle, 28f, 56f, subtitlePaint)
            }

            drawHeader()
            var y = 104f
            lines.forEach { line ->
                if (y > pageHeight - 40f) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    drawHeader()
                    y = 104f
                }
                canvas.drawText(line.take(92), 28f, y, linePaint)
                canvas.drawLine(28f, y + 6f, pageWidth - 28f, y + 6f, dividerPaint)
                y += 22f
            }
            document.finishPage(page)
            FileOutputStream(file).use { out -> document.writeTo(out) }
            document.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
            true
        }.getOrDefault(false)
    }
}
