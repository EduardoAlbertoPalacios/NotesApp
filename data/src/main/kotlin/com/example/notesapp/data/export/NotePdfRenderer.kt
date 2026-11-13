package com.example.notesapp.data.export

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.TextFormat
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Dibuja una nota en un PDF de páginas A4, siempre con fondo blanco y texto negro, sin mirar el tema ni el
 * color de la nota (spec 011, RF-32 a RF-44).
 *
 * Primero va el texto (título, categoría, fecha y contenido) en un único [StaticLayout] que se reparte en
 * páginas línea a línea, y después las imágenes, una por una.
 */
internal class NotePdfRenderer(
    private val categoryLabel: (Note) -> String?,
    private val locale: Locale = Locale.getDefault(),
) {

    /** Escribe el PDF de [note] en [output] y devuelve cuántas imágenes no se pudieron leer y se omitieron. */
    suspend fun render(note: Note, output: OutputStream): Int {
        val document = PdfDocument()
        try {
            val pages = PageCursor(document)
            drawText(buildText(note), pages)
            val missingImages = drawImages(note, pages)
            pages.finish()
            document.writeTo(output)
            return missingImages
        } finally {
            document.close()
        }
    }

    // region Texto

    private fun buildText(note: Note): CharSequence {
        val text = SpannableStringBuilder()
        // RF-32, RF-33: el título solo aparece si la nota lo tiene.
        if (note.title.isNotBlank()) {
            text.appendStyled(note.title.trim(), StyleSpan(Typeface.BOLD), AbsoluteSizeSpan(TITLE_TEXT_SIZE))
            text.append('\n')
        }
        // RF-34
        categoryLabel(note)?.let { text.appendStyled(it, AbsoluteSizeSpan(META_TEXT_SIZE)).append('\n') }
        // RF-35: fecha siempre absoluta, porque el PDF se lee después.
        text.appendStyled(formatDate(note.updatedAt), AbsoluteSizeSpan(META_TEXT_SIZE))
        if (note.content.isNotEmpty()) {
            text.append("\n\n")
            appendContent(text, note.content, note.contentSpans, note.contentTasks)
        }
        return text
    }

    private fun formatDate(epochMillis: Long): String {
        val date = Date(epochMillis)
        val day = SimpleDateFormat(DATE_PATTERN, locale).format(date)
        val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(date)
        return "$day, $time"
    }

    /**
     * Añade el contenido con su formato (RF-36) y con «☐ » o «☑ » al inicio de cada línea de tarea (RF-37).
     *
     * Los prefijos desplazan el texto, así que los [FormatSpan], que cuentan sobre el contenido original, se
     * traducen con [ContentOffsets]. El prefijo nunca queda dentro de un tramo con formato.
     */
    private fun appendContent(
        text: SpannableStringBuilder,
        content: String,
        spans: List<FormatSpan>,
        tasks: List<ChecklistItem>,
    ) {
        val base = text.length
        val lines = content.split('\n')
        val taskByLine = tasks.associateBy { it.line }
        lines.forEachIndexed { index, line ->
            if (index > 0) text.append('\n')
            taskByLine[index]?.let { text.append(if (it.isDone) DONE_PREFIX else PENDING_PREFIX) }
            text.append(line)
        }
        val offsets = ContentOffsets(lines, taskByLine.keys, PREFIX_LENGTH)
        spans.forEach { span ->
            val start = span.start.coerceIn(0, content.length)
            val end = span.end.coerceIn(0, content.length)
            if (start >= end) return@forEach
            text.setSpan(
                span.format.toStyleSpan(),
                base + offsets.mapStart(start),
                base + offsets.mapEnd(end),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    private fun TextFormat.toStyleSpan(): Any = when (this) {
        TextFormat.BOLD -> StyleSpan(Typeface.BOLD)
        TextFormat.UNDERLINE -> UnderlineSpan()
        TextFormat.STRIKETHROUGH -> StrikethroughSpan()
    }

    private fun SpannableStringBuilder.appendStyled(value: String, vararg styles: Any): SpannableStringBuilder {
        val start = length
        append(value)
        styles.forEach { setSpan(it, start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        return this
    }

    /**
     * RF-41: recorre las líneas del layout y las agrupa en tramos que caben en la página actual. Cuando la
     * siguiente línea no cabe, dibuja el tramo y abre una página nueva.
     */
    private suspend fun drawText(text: CharSequence, pages: PageCursor) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = BODY_TEXT_SIZE
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        var first = 0
        for (line in 0 until layout.lineCount) {
            val chunkHeight = layout.getLineBottom(line) - layout.getLineTop(first)
            // Una línea que no cabe ni en una página vacía se dibuja igualmente (recortada) para no entrar en
            // un bucle de páginas vacías.
            val fits = chunkHeight <= pages.remaining || (line == first && pages.isEmpty)
            if (!fits) {
                if (line > first) drawLines(layout, first, line, pages)
                pages.newPage()
                currentCoroutineContext().ensureActive()
                first = line
            }
        }
        if (first < layout.lineCount) drawLines(layout, first, layout.lineCount, pages)
    }

    /** Dibuja las líneas de [first] (incluida) a [end] (excluida) en la posición actual de [pages]. */
    private fun drawLines(layout: StaticLayout, first: Int, end: Int, pages: PageCursor) {
        val top = layout.getLineTop(first)
        val bottom = layout.getLineTop(end)
        val canvas = pages.canvas
        canvas.save()
        canvas.translate(MARGIN.toFloat(), pages.y - top)
        // El recorte limita el dibujo a estas líneas: el layout solo pinta las que caen dentro.
        canvas.clipRect(0, top, CONTENT_WIDTH, bottom)
        layout.draw(canvas)
        canvas.restore()
        pages.advance((bottom - top).toFloat())
    }

    // endregion

    // region Imágenes

    /** RF-38: imágenes después del texto, en el orden de la nota. Devuelve cuántas se omitieron (RF-44). */
    private suspend fun drawImages(note: Note, pages: PageCursor): Int {
        var missing = 0
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        note.images.forEach { image ->
            currentCoroutineContext().ensureActive()
            if (!drawImage(image.path, pages, paint)) missing++
        }
        return missing
    }

    /** Dibuja la imagen de [path]; devuelve `false` si no existe o no se puede decodificar. */
    private fun drawImage(path: String, pages: PageCursor, paint: Paint): Boolean {
        if (!File(path).isFile) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        // Orientación EXIF como matriz sobre la imagen sin girar, y tamaño que ocupa ya girada.
        val orientation = orientationMatrix(path)
        val oriented = RectF(0f, 0f, bounds.outWidth.toFloat(), bounds.outHeight.toFloat())
        orientation.mapRect(oriented)

        // RF-39: al ancho útil sin deformar; RF-40: si no cabe en el alto útil, se reduce hasta que quepa.
        var drawWidth = CONTENT_WIDTH.toFloat()
        var drawHeight = oriented.height() * drawWidth / oriented.width()
        if (drawHeight > CONTENT_HEIGHT) {
            drawHeight = CONTENT_HEIGHT.toFloat()
            drawWidth = oriented.width() * drawHeight / oriented.height()
        }

        // Se decodifica con la resolución justa para el tamaño en que se dibuja (unos 1240 px a todo el
        // ancho útil), para no cargar en memoria ni incrustar en el PDF la imagen original completa.
        val targetPixels = drawWidth * PIXELS_PER_POINT
        // inSampleSize reduce igual ambos lados, así que basta con mirar el que acaba siendo el ancho.
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(oriented.width(), targetPixels)
        }
        val bitmap = BitmapFactory.decodeFile(path, decodeOptions) ?: return false
        try {
            val gap = if (pages.isEmpty) 0f else IMAGE_GAP
            if (gap + drawHeight > pages.remaining && !pages.isEmpty) {
                pages.newPage()
            } else {
                pages.advance(gap)
            }
            // La matriz final: orientación EXIF, mover al origen, escalar al tamaño de dibujo y colocar.
            val matrix = Matrix().apply {
                // El bitmap viene reducido por inSampleSize: se vuelve a la escala original antes de orientar.
                setScale(bounds.outWidth / bitmap.width.toFloat(), bounds.outHeight / bitmap.height.toFloat())
                postConcat(orientation)
                postTranslate(-oriented.left, -oriented.top)
                postScale(drawWidth / oriented.width(), drawHeight / oriented.height())
                postTranslate(MARGIN + (CONTENT_WIDTH - drawWidth) / 2f, pages.y)
            }
            pages.canvas.drawBitmap(bitmap, matrix, paint)
            pages.advance(drawHeight)
        } finally {
            bitmap.recycle()
        }
        return true
    }

    /** Mayor potencia de 2 que deja el ancho decodificado por encima de [targetPixels]. */
    private fun sampleSize(sourceWidth: Float, targetPixels: Float): Int {
        var sample = 1
        while (sourceWidth / (sample * 2) >= targetPixels) sample *= 2
        return sample
    }

    /**
     * Matriz que aplica la orientación EXIF de la imagen, sin traslación (el giro deja la imagen fuera del
     * origen; quien la usa la recoloca). Si no se puede leer el EXIF, la imagen se dibuja tal cual.
     */
    private fun orientationMatrix(path: String): Matrix {
        val orientation = try {
            ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (error: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
    }

    // endregion

    /** Página actual del documento y posición vertical donde sigue el contenido. */
    private class PageCursor(private val document: PdfDocument) {
        private var number = 0
        private var page: PdfDocument.Page = start()

        var y: Float = MARGIN.toFloat()
            private set

        val canvas get() = page.canvas
        val remaining: Float get() = PAGE_HEIGHT - MARGIN - y
        val isEmpty: Boolean get() = y == MARGIN.toFloat()

        fun advance(height: Float) {
            y += height
        }

        fun newPage() {
            document.finishPage(page)
            page = start()
        }

        fun finish() = document.finishPage(page)

        private fun start(): PdfDocument.Page {
            number++
            y = MARGIN.toFloat()
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()
            // RF-42: fondo blanco explícito, sea cual sea el visor.
            return document.startPage(info).apply { canvas.drawColor(Color.WHITE) }
        }
    }

    /**
     * Traduce posiciones del contenido original a posiciones del texto con los prefijos de tarea insertados
     * al inicio de las líneas [taskLines].
     */
    private class ContentOffsets(lines: List<String>, taskLines: Set<Int>, private val prefixLength: Int) {
        /** Posición, en el contenido original, donde empieza cada línea. */
        private val lineStarts = IntArray(lines.size)

        /** Prefijos insertados hasta cada línea, ella incluida. */
        private val prefixesUpTo = IntArray(lines.size)

        init {
            var start = 0
            var prefixes = 0
            lines.forEachIndexed { index, line ->
                lineStarts[index] = start
                if (index in taskLines) prefixes++
                prefixesUpTo[index] = prefixes
                start += line.length + 1
            }
        }

        /** Posición del carácter [offset]: queda después del prefijo de su línea y de los anteriores. */
        fun mapStart(offset: Int): Int = offset + prefixLength * prefixesUpTo[lineOf(offset)]

        /** Fin exclusivo: justo detrás del último carácter incluido, así no abarca el prefijo siguiente. */
        fun mapEnd(offset: Int): Int = mapStart(offset - 1) + 1

        private fun lineOf(offset: Int): Int {
            val index = lineStarts.binarySearch(offset)
            return if (index >= 0) index else -index - 2
        }
    }

    private companion object {
        /** A4 a 72 puntos por pulgada (RF-43). */
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 48
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        const val CONTENT_HEIGHT = PAGE_HEIGHT - 2 * MARGIN

        const val BODY_TEXT_SIZE = 12f
        const val TITLE_TEXT_SIZE = 20
        const val META_TEXT_SIZE = 10
        const val IMAGE_GAP = 12f

        /** Unos 1240 px a todo el ancho útil (~180 ppp): nítido al imprimir sin inflar el PDF. */
        const val TARGET_IMAGE_WIDTH_PX = 1240f
        const val PIXELS_PER_POINT = TARGET_IMAGE_WIDTH_PX / CONTENT_WIDTH

        const val DATE_PATTERN = "d MMM yyyy"
        const val PENDING_PREFIX = "☐ "
        const val DONE_PREFIX = "☑ "
        const val PREFIX_LENGTH = 2
    }
}
