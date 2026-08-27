package dev.basefile.future.roborazzi

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PdfIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `PDFBox creates loads inspects renders and rejects corruption`() {
        val file = File(context.cacheDir, "generated.pdf")
        PDDocument().use { document ->
            document.documentInformation.title = "Offline PDF"
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 14f)
                stream.newLineAtOffset(72f, 700f)
                stream.showText("Offline PDF fixture")
                stream.endText()
            }
            document.save(file)
        }
        PDDocument.load(file).use { loaded ->
            assertEquals(1, loaded.numberOfPages)
            assertEquals("Offline PDF", loaded.documentInformation.title)
            val bitmap = PDFRenderer(loaded).renderImageWithDPI(0, 72f)
            assertTrue(bitmap.width > 100 && bitmap.height > 100)
        }

        val corrupt = File(context.cacheDir, "corrupt.pdf").apply {
            writeBytes(file.readBytes().copyOfRange(0, 24))
        }
        assertThrows(IOException::class.java) { PDDocument.load(corrupt) }
    }

    @Test
    fun `password protection and framework PdfDocument paths execute`() {
        val protected = File(context.cacheDir, "protected.pdf")
        PDDocument().use { document ->
            document.addPage(PDPage())
            document.protect(StandardProtectionPolicy("owner-secret", "reader-secret", AccessPermission()))
            document.save(protected)
        }
        assertThrows(InvalidPasswordException::class.java) { PDDocument.load(protected) }
        PDDocument.load(protected, "reader-secret").use { assertEquals(1, it.numberOfPages) }

        val frameworkFile = File(context.cacheDir, "framework.pdf")
        PdfDocument().use { document ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(300, 300, 1).create())
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawText("Framework PDF", 24f, 80f, Paint().apply { color = Color.BLACK })
            document.finishPage(page)
            frameworkFile.outputStream().use(document::writeTo)
        }
        assertTrue(frameworkFile.length() > 100)
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun initializePdfBox() {
            PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
        }
    }
}
