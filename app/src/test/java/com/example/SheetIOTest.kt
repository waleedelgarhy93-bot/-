package com.example

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.util.ExportTable
import com.example.util.SheetIO
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SheetIOTest {

    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun xlsxRoundTripAndStudentParsing() {
        val table = ExportTable(
            title = "مجموعة السبت",
            subtitle = "نوفمبر 2026",
            headers = listOf("م", "الاسم", "ولي الأمر"),
            rows = listOf(listOf(1, "أحمد محمد", "01012345678"), listOf(2, "سارة & علي", "01122223333")),
            weights = listOf(0.6f, 3f, 2f)
        )
        val f = File("build/test-export.xlsx")
        f.outputStream().use { SheetIO.writeXlsx(table, it) }
        val rows = SheetIO.readRows(ctx, Uri.fromFile(f.absoluteFile))
        assertEquals("مجموعة السبت", rows[0][0])
        val students = SheetIO.parseStudents(rows.drop(3)) // header row onward
        assertEquals(listOf("أحمد محمد", "سارة & علي"), students.map { it.name })
        assertEquals("01012345678", students[0].parentPhone)
    }

    @Test
    fun csvWithoutHeader() {
        val f = File("build/test.csv")
        f.writeText("1,محمود علي,1001234567\n2,منى حسن,01009998888\n", Charsets.UTF_8)
        val s = SheetIO.parseStudents(SheetIO.readRows(ctx, Uri.fromFile(f.absoluteFile)))
        assertEquals(listOf("محمود علي", "منى حسن"), s.map { it.name })
        assertEquals("01001234567", s[0].parentPhone)
    }
}
