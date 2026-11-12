package com.vukos.trening.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Slike tela:
 *  - cuvaju se SAMO u internom skladistu aplikacije (filesDir/body_photos), koje druge aplikacije
 *    ne mogu da citaju, ne pojavljuju se u galeriji i ne idu u backup (allowBackup=false);
 *  - dodatno su sifrovane AES-256-GCM kljucem iz Android Keystore-a (kljuc ne moze da se izvuce
 *    sa telefona), pa su necitljive cak i ako neko kopira fajlove;
 *  - ne salju se u Notion.
 */
object SecurePhotoStore {
    private const val KEY_ALIAS = "trening_body_photos"
    private const val MAX_SIDE = 1600

    private fun dir(context: Context) = File(context.filesDir, "body_photos").apply { mkdirs() }

    /** Ucita sliku (kamera ili galerija), smanji na max 1600 px, sifruje i vrati putanju .enc fajla. */
    fun importImage(context: Context, uri: Uri, id: String): String {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            val (w, h) = info.size.width to info.size.height
            val scale = MAX_SIDE.toFloat() / maxOf(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val jpeg = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out); out.toByteArray()
        }
        bitmap.recycle()
        val file = File(dir(context), "$id.enc")
        file.writeBytes(encrypt(jpeg))
        return file.absolutePath
    }

    fun load(path: String): Bitmap? = runCatching {
        val bytes = decrypt(File(path).readBytes())
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    fun delete(path: String) { File(path).delete() }

    private val crypto = KeystoreCrypto(KEY_ALIAS)
    fun encrypt(plain: ByteArray): ByteArray = crypto.encrypt(plain)
    fun decrypt(data: ByteArray): ByteArray = crypto.decrypt(data)
}

/** Tri poze za slike tela. */
enum class Pose(val key: String, val label: String) {
    FRONT("front", "Spreda"),
    SIDE("side", "Bok"),
    BACK("back", "Leđa"),
}
