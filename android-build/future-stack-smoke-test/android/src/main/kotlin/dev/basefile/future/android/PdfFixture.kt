package dev.basefile.future.android

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import java.io.File

object PdfFixture {
    fun createOnePage(context: Context, target: File) {
        PDFBoxResourceLoader.init(context)
        PDDocument().use { document ->
            document.documentInformation.title = "Offline fixture"
            document.addPage(PDPage())
            document.save(target)
        }
    }
}
