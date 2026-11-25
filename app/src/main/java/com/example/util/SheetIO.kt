package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.util.Xml
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.repository.ImportedStudent
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A simple table that can be written as Excel or PDF. Cells that are [Number] are written as numbers. */
class ExportTable(
    val title: String,
    val subtitle: String,
    val headers: List<String>,
    val rows: List<List<Any>>,
    /** Relative column widths, same size as [headers]. */
    val weights: List<Float>,
    val footer: String = ""
)

class OldExcelFormatException : Exception()

object SheetIO {

    // ------------------------------------------------------------------ reading

    /** Reads the first sheet of an .xlsx file, or a .csv file, as rows of text cells. */
    fun readRows(context: Context, uri: Uri): List<List<String>> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("cannot open file")
        if (bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) return readXlsx(bytes)
        if (bytes.size >= 4 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte()) throw OldExcelFormatException()
        return readCsv(decodeText(bytes))
    }

    private fun decodeText(bytes: ByteArray): String {
        val start = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) 3 else 0
        val buf = ByteBuffer.wrap(bytes, start, bytes.size - start)
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(buf).toString()
        } catch (_: CharacterCodingException) {
            // Arabic CSV saved by old Excel on Windows
            String(bytes, start, bytes.size - start, Charset.forName("windows-1256"))
        }
    }

    private fun readCsv(text: String): List<List<String>> {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val sep = listOf(',', ';', '\t').maxByOrNull { c -> firstLine.count { it == c } } ?: ','
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                quoted && ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                !quoted && ch == sep -> { row.add(cell.toString().trim()); cell.clear() }
                !quoted && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString().trim()); cell.clear()
                    rows.add(row); row = ArrayList()
                }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString().trim()); rows.add(row) }
        return rows
    }

    private fun readXlsx(bytes: ByteArray): List<List<String>> {
        val files = HashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val n = e.name.trimStart('/')
                if (n == "xl/sharedStrings.xml" || n == "xl/workbook.xml" || n == "xl/_rels/workbook.xml.rels" ||
                    n.startsWith("xl/worksheets/sheet")
                ) files[n] = zip.readBytes()
                e = zip.nextEntry
            }
        }
        val shared = files["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) }.orEmpty()
        val sheetPath = firstSheetPath(files)
            ?: files.keys.filter { it.startsWith("xl/worksheets/sheet") }.minOrNull()
            ?: error("no sheet")
        return parseSheet(files.getValue(sheetPath), shared)
    }

    private fun parser(data: ByteArray): XmlPullParser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(ByteArrayInputStream(data), "UTF-8")
    }

    private fun firstSheetPath(files: Map<String, ByteArray>): String? {
        val wb = files["xl/workbook.xml"] ?: return null
        val rels = files["xl/_rels/workbook.xml.rels"] ?: return null
        var rid: String? = null
        val p = parser(wb)
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name.endsWith("sheet") && p.name != "sheets") {
                rid = (0 until p.attributeCount).firstOrNull { p.getAttributeName(it).endsWith(":id") }?.let { p.getAttributeValue(it) }
                break
            }
        }
        rid ?: return null
        val r = parser(rels)
        while (r.next() != XmlPullParser.END_DOCUMENT) {
            if (r.eventType == XmlPullParser.START_TAG && r.name == "Relationship" && r.getAttributeValue(null, "Id") == rid) {
                val target = r.getAttributeValue(null, "Target") ?: return null
                val path = if (target.startsWith("/")) target.trimStart('/') else "xl/$target"
                return path.takeIf { files.containsKey(it) }
            }
        }
        return null
    }

    private fun parseSharedStrings(data: ByteArray): List<String> {
        val out = ArrayList<String>()
        val p = parser(data)
        val sb = StringBuilder()
        var inSi = false
        var inT = false
        var inPhonetic = false
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> { inSi = true; sb.clear() }
                    "t" -> inT = true
                    "rPh" -> inPhonetic = true
                }
                XmlPullParser.TEXT -> if (inSi && inT && !inPhonetic) sb.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "si" -> { out.add(sb.toString()); inSi = false }
                    "t" -> inT = false
                    "rPh" -> inPhonetic = false
                }
            }
        }
        return out
    }

    private fun columnIndex(ref: String): Int {
        var n = 0
        for (c in ref) {
            if (!c.isLetter()) break
            n = n * 26 + (c.uppercaseChar() - 'A' + 1)
        }
        return n - 1
    }

    private fun parseSheet(data: ByteArray, shared: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val p = parser(data)
        var row: MutableMap<Int, String>? = null
        var col = -1
        var type: String? = null
        var nextCol = 0
        val value = StringBuilder()
        var inValue = false
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> { row = HashMap(); nextCol = 0 }
                    "c" -> {
                        val ref = p.getAttributeValue(null, "r")
                        col = if (ref != null) columnIndex(ref) else nextCol
                        type = p.getAttributeValue(null, "t")
                        value.clear()
                    }
                    "v", "t" -> inValue = true
                }
                XmlPullParser.TEXT -> if (inValue) value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        val raw = value.toString()
                        val text = when (type) {
                            "s" -> raw.trim().toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty()
                            "inlineStr", "str" -> raw
                            "b" -> if (raw == "1") "TRUE" else "FALSE"
                            else -> plainNumber(raw)
                        }
                        if (col >= 0) row?.put(col, text.trim())
                        nextCol = col + 1
                    }
                    "row" -> {
                        val r = row ?: continue
                        val max = r.keys.maxOrNull() ?: -1
                        rows.add((0..max).map { r[it].orEmpty() })
                        row = null
                    }
                }
            }
        }
        return rows
    }

    /** "1.012345678E9" -> "1012345678", "5.0" -> "5". */
    private fun plainNumber(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return t
        return runCatching { BigDecimal(t).stripTrailingZeros().toPlainString() }.getOrDefault(t)
    }

    // ------------------------------------------------------------------ students from rows

    private val nameKeys = listOf("اسم", "name", "الطالب", "طالب")
    private val parentKeys = listOf("ولي", "الأمر", "الامر", "parent", "والد", "الأب", "الاب", "الأم", "الام")
    private val phoneKeys = listOf("تليفون", "تلفون", "موبايل", "هاتف", "رقم", "phone", "mobile", "tel", "واتس")

    private fun hasLetters(s: String) = s.any { it.isLetter() }
    private fun isPhone(s: String) = s.count { it.isDigit() } >= 8 && !hasLetters(s)

    fun normalizePhone(s: String): String {
        val d = s.filter { it.isDigit() || it == '+' }
        return if (d.length == 10 && d.startsWith("1")) "0$d" else d
    }

    /** Guesses which columns hold the name and phones (with or without a header row). */
    fun parseStudents(allRows: List<List<String>>): List<ImportedStudent> {
        val rows = allRows.filter { r -> r.any { it.isNotBlank() } }
        if (rows.isEmpty()) return emptyList()
        val first = rows.first().map { it.lowercase() }
        val headerName = first.indexOfFirst { c -> nameKeys.any { c.contains(it) } && parentKeys.none { c.contains(it) } && phoneKeys.none { c.contains(it) } }
        val headerParent = first.indexOfFirst { c -> parentKeys.any { c.contains(it) } }
        val headerPhone = first.indexOfFirst { c -> phoneKeys.any { c.contains(it) } && parentKeys.none { c.contains(it) } }
        val hasHeader = headerName >= 0 || headerParent >= 0 || headerPhone >= 0
        val body = if (hasHeader) rows.drop(1) else rows
        val width = body.maxOfOrNull { it.size } ?: 0

        val nameCol = if (headerName >= 0) headerName else
            (0 until width).maxByOrNull { c -> body.count { hasLetters(it.getOrElse(c) { "" }) } } ?: 0
        val phoneCols = (0 until width).filter { c -> c != nameCol && body.count { isPhone(it.getOrElse(c) { "" }) } * 2 >= body.size.coerceAtLeast(1) }
        val parentCol = when {
            headerParent >= 0 -> headerParent
            else -> phoneCols.firstOrNull() ?: -1
        }
        val studentCol = when {
            headerPhone >= 0 && headerPhone != parentCol -> headerPhone
            else -> phoneCols.firstOrNull { it != parentCol } ?: -1
        }
        return body.mapNotNull { r ->
            val name = r.getOrElse(nameCol) { "" }.trim()
            if (!hasLetters(name)) return@mapNotNull null
            ImportedStudent(
                name = name,
                parentPhone = if (parentCol >= 0) normalizePhone(r.getOrElse(parentCol) { "" }) else "",
                studentPhone = if (studentCol >= 0) normalizePhone(r.getOrElse(studentCol) { "" }) else ""
            )
        }
    }

    // ------------------------------------------------------------------ Excel writing

    private fun esc(s: String) = buildString {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            else -> if (c.code >= 0x20 || c == '\t' || c == '\n') append(c)
        }
    }

    private fun colName(i: Int): String {
        var n = i + 1
        val sb = StringBuilder()
        while (n > 0) { val m = (n - 1) % 26; sb.insert(0, ('A' + m)); n = (n - 1) / 26 }
        return sb.toString()
    }

    fun writeXlsx(table: ExportTable, out: OutputStream) {
        val sheet = StringBuilder()
        sheet.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sheet.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sheet.append("""<sheetViews><sheetView workbookViewId="0" rightToLeft="1"/></sheetViews>""")
        sheet.append("<cols>")
        table.weights.forEachIndexed { i, w ->
            val width = (w * 6f).coerceIn(5f, 45f)
            sheet.append("""<col min="${i + 1}" max="${i + 1}" width="$width" customWidth="1"/>""")
        }
        sheet.append("</cols><sheetData>")
        var r = 0
        fun row(cells: List<Any>, style: Int) {
            r++
            sheet.append("""<row r="$r">""")
            cells.forEachIndexed { c, v ->
                val ref = "${colName(c)}$r"
                if (v is Number) sheet.append("""<c r="$ref" s="$style"><v>$v</v></c>""")
                else sheet.append("""<c r="$ref" s="$style" t="inlineStr"><is><t xml:space="preserve">${esc(v.toString())}</t></is></c>""")
            }
            sheet.append("</row>")
        }
        row(listOf(table.title), 1)
        if (table.subtitle.isNotBlank()) row(listOf(table.subtitle), 0)
        r++ // empty row
        row(table.headers, 2)
        table.rows.forEach { row(it, 3) }
        if (table.footer.isNotBlank()) { r++; row(listOf(table.footer), 1) }
        sheet.append("</sheetData></worksheet>")

        val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="12"/><name val="Calibri"/></font></fonts>
<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFDCE8F5"/><bgColor indexed="64"/></patternFill></fill></fills>
<borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"><color rgb="FFB0B0B0"/></left><right style="thin"><color rgb="FFB0B0B0"/></right><top style="thin"><color rgb="FFB0B0B0"/></top><bottom style="thin"><color rgb="FFB0B0B0"/></bottom><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="4">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""
        val files = linkedMapOf(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""",
            "xl/styles.xml" to styles,
            "xl/worksheets/sheet1.xml" to sheet.toString()
        )
        ZipOutputStream(out).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    // ------------------------------------------------------------------ PDF writing

    /** Landscape A4, right-to-left table, header repeated on every page. */
    fun writePdf(context: Context, table: ExportTable, out: OutputStream) {
        val pageW = 842; val pageH = 595
        val margin = 28f
        val rowH = 22f
        val font = runCatching { ResourcesCompat.getFont(context, R.font.cairo) }.getOrNull() ?: Typeface.DEFAULT
        val bold = Typeface.create(font, Typeface.BOLD)
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; textSize = 9.5f; color = Color.rgb(30, 30, 30) }
        val head = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 10f; color = Color.rgb(20, 40, 80) }
        val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = 16f; color = Color.rgb(20, 40, 80) }
        val subP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; textSize = 10f; color = Color.DKGRAY }
        val line = Paint().apply { color = Color.rgb(190, 190, 190); strokeWidth = 0.6f; style = Paint.Style.STROKE }
        val headFill = Paint().apply { color = Color.rgb(220, 232, 245) }
        val zebra = Paint().apply { color = Color.rgb(246, 248, 251) }

        val tableW = pageW - 2 * margin
        val total = table.weights.sum().coerceAtLeast(1f)
        val widths = table.weights.map { it / total * tableW }

        fun text(c: Canvas, s: String, p: TextPaint, right: Float, top: Float, w: Float, h: Float, align: Layout.Alignment) {
            val width = w.toInt().coerceAtLeast(4) - 4
            val layout = StaticLayout.Builder.obtain(s, 0, s.length, p, width)
                .setAlignment(align)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setMaxLines(1)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(false)
                .build()
            c.save()
            c.translate(right - w + 2f, top + (h - layout.height) / 2f)
            layout.draw(c)
            c.restore()
        }

        fun drawRow(c: Canvas, cells: List<Any>, y: Float, isHead: Boolean, shade: Boolean) {
            if (isHead) c.drawRect(margin, y, pageW - margin, y + rowH, headFill)
            else if (shade) c.drawRect(margin, y, pageW - margin, y + rowH, zebra)
            var right = pageW - margin
            widths.forEachIndexed { i, w ->
                val v = cells.getOrNull(i)?.let { if (it is Double) Formatters.formatAmount(it) else it.toString() }.orEmpty()
                text(c, v, if (isHead) head else body, right, y, w, rowH, Layout.Alignment.ALIGN_CENTER)
                c.drawRect(right - w, y, right, y + rowH, line)
                right -= w
            }
        }

        val doc = PdfDocument()
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            val c = page!!.canvas
            y = margin
            if (pageNo == 1) {
                text(c, table.title, titleP, pageW - margin, y, tableW, 24f, Layout.Alignment.ALIGN_NORMAL)
                y += 24f
                if (table.subtitle.isNotBlank()) {
                    text(c, table.subtitle, subP, pageW - margin, y, tableW, 16f, Layout.Alignment.ALIGN_NORMAL)
                    y += 18f
                }
                y += 6f
            }
            drawRow(c, table.headers, y, isHead = true, shade = false)
            y += rowH
            text(c, "$pageNo", subP, pageW - margin, pageH - margin + 6f, tableW, 14f, Layout.Alignment.ALIGN_CENTER)
        }
        newPage()
        table.rows.forEachIndexed { i, r ->
            if (y + rowH > pageH - margin - 12f) newPage()
            drawRow(page!!.canvas, r, y, isHead = false, shade = i % 2 == 1)
            y += rowH
        }
        if (table.footer.isNotBlank()) {
            if (y + 26f > pageH - margin - 12f) newPage()
            text(page!!.canvas, table.footer, head, pageW - margin, y + 6f, tableW, 18f, Layout.Alignment.ALIGN_NORMAL)
        }
        doc.finishPage(page!!)
        doc.writeTo(out)
        doc.close()
    }
}
