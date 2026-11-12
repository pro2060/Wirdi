package com.proart.wirdi

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.DocumentsContract

object Mushaf {
    private var idxKey: String? = null
    private var idx: Map<Int, Uri> = emptyMap()

    fun reset() = synchronized(this) { idxKey = null; idx = emptyMap() }

    fun isConfigured(c: Context): Boolean =
        Prefs.getMushafType(c) != 0 && Prefs.getMushafUri(c) != null

    @Synchronized
    private fun index(c: Context, uriStr: String): Map<Int, Uri> {
        if (idxKey == uriStr && idx.isNotEmpty()) return idx
        idx = buildIndex(c, Uri.parse(uriStr))
        idxKey = uriStr
        return idx
    }

    private fun buildIndex(c: Context, tree: Uri): Map<Int, Uri> {
        val map = HashMap<Int, Uri>()
        val docId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val cur: Cursor? = c.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null
        )
        if (cur != null) {
            try {
                while (cur.moveToNext()) {
                    val id = cur.getString(0) ?: continue
                    val name = cur.getString(1) ?: continue
                    val mime = cur.getString(2) ?: ""
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val isImage = mime.startsWith("image/") || ext in listOf("png", "jpg", "jpeg", "webp")
                    if (!isImage) continue
                    val base = name.substringBeforeLast('.')
                    val digits = Regex("\\d+").findAll(base).lastOrNull()?.value ?: continue
                    val n = digits.toIntOrNull() ?: continue
                    map[n] = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                }
            } finally {
                cur.close()
            }
        }
        return map
    }

    /** عدد الصفحات المتاحة (صور في الفولدر أو صفحات الـ PDF). */
    fun pageCount(c: Context): Int {
        val s = Prefs.getMushafUri(c) ?: return 0
        return try {
            when (Prefs.getMushafType(c)) {
                1 -> index(c, s).size
                2 -> {
                    val pfd = c.contentResolver.openFileDescriptor(Uri.parse(s), "r") ?: return 0
                    try {
                        val r = PdfRenderer(pfd)
                        val n = r.pageCount
                        r.close()
                        n
                    } finally {
                        pfd.close()
                    }
                }
                else -> 0
            }
        } catch (e: Throwable) {
            0
        }
    }

    /** يحمّل صفحة المصحف كصورة (يُنادى من خيط خلفي). */
    fun load(c: Context, page: Int, targetW: Int): Bitmap? {
        val s = Prefs.getMushafUri(c) ?: return null
        val offset = Prefs.getOffset(c)
        return try {
            when (Prefs.getMushafType(c)) {
                1 -> {
                    val u = index(c, s)[page + offset] ?: return null
                    decode(c, u, targetW)
                }
                2 -> renderPdf(c, Uri.parse(s), page - 1 + offset, targetW)
                else -> null
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun decode(c: Context, uri: Uri, targetW: Int): Bitmap? {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        val s1 = c.contentResolver.openInputStream(uri) ?: return null
        try { BitmapFactory.decodeStream(s1, null, bounds) } finally { s1.close() }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetW) sample *= 2
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        val s2 = c.contentResolver.openInputStream(uri) ?: return null
        return try { BitmapFactory.decodeStream(s2, null, opts) } finally { s2.close() }
    }

    private fun renderPdf(c: Context, uri: Uri, pageIndex: Int, targetW: Int): Bitmap? {
        val pfd = c.contentResolver.openFileDescriptor(uri, "r") ?: return null
        try {
            val r = PdfRenderer(pfd)
            try {
                if (pageIndex < 0 || pageIndex >= r.pageCount) return null
                val p = r.openPage(pageIndex)
                try {
                    val w = targetW
                    val h = (w.toFloat() * p.height / p.width).toInt()
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bmp
                } finally {
                    p.close()
                }
            } finally {
                r.close()
            }
        } finally {
            pfd.close()
        }
    }
}
