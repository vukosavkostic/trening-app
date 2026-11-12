package com.vukos.trening.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
    private const val IV_LEN = 12

    private fun dir(context: Context) = File(context.filesDir, "body_photos").apply { mkdirs() }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

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

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data, 0, IV_LEN))
        return cipher.doFinal(data, IV_LEN, data.size - IV_LEN)
    }
}

/** Tri poze za slike tela. */
enum class Pose(val key: String, val label: String) {
    FRONT("front", "Spreda"),
    SIDE("side", "Bok"),
    BACK("back", "Leđa"),
}
