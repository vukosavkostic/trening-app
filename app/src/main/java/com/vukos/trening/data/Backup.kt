package com.vukos.trening.data

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.vukos.trening.TreningApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupException(message: String) : Exception(message)

/**
 * Format rezervne kopije: "TRBK1" + so (16) + IV (12) + AES-256-GCM(zip).
 * Kljuc se dobija iz lozinke (PBKDF2-HMAC-SHA256). Bez lozinke se fajl ne moze procitati.
 */
object BackupCrypto {
    private val MAGIC = "TRBK1".toByteArray()
    private const val ITERATIONS = 150_000

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also(rnd::nextBytes)
        val iv = ByteArray(12).also(rnd::nextBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        c.updateAAD(MAGIC)
        return MAGIC + salt + iv + c.doFinal(plain)
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        if (data.size < MAGIC.size + 28 || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC))
            throw BackupException("Ovo nije rezervna kopija Trening aplikacije.")
        var p = MAGIC.size
        val salt = data.copyOfRange(p, p + 16); p += 16
        val iv = data.copyOfRange(p, p + 12); p += 12
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        c.updateAAD(MAGIC)
        return try { c.doFinal(data, p, data.size - p) } catch (e: AEADBadTagException) {
            throw BackupException("Pogrešna lozinka (ili je fajl oštećen).")
        }
    }
}

/** Izvoz i uvoz svih podataka: baza + slike tela (desifrovane unutar sifrovanog fajla) + slike sopstvenih vezbi. */
object Backup {
    private const val DB_ENTRY = "trening.db"
    private const val PHOTOS = "body_photos"
    private const val EX_IMAGES = "exercise_images"

    suspend fun export(context: Context, target: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val app = TreningApp.instance
        // Sve iz WAL-a upisi u glavni fajl baze pre kopiranja.
        app.db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val zipBytes = ByteArrayOutputStream().use { bos ->
            ZipOutputStream(bos).use { zip ->
                fun put(name: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
                put(DB_ENTRY, context.getDatabasePath(AppDatabase.NAME).readBytes())
                File(context.filesDir, PHOTOS).listFiles()?.forEach { f ->
                    runCatching { SecurePhotoStore.decrypt(f.readBytes()) }.onSuccess { put("$PHOTOS/${f.name}", it) }
                }
                File(context.filesDir, EX_IMAGES).listFiles()?.forEach { f -> put("$EX_IMAGES/${f.name}", f.readBytes()) }
            }
            bos.toByteArray()
        }
        val out = BackupCrypto.encrypt(zipBytes, password)
        context.contentResolver.openOutputStream(target, "wt")!!.use { it.write(out) }
        app.settings.lastBackupAt = System.currentTimeMillis()
    }

    /** Zamenjuje SVE podatke na telefonu sadrzajem kopije, pa ponovo pokrece aplikaciju. */
    suspend fun import(context: Context, source: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val data = context.contentResolver.openInputStream(source)!!.use { it.readBytes() }
        val zipBytes = BackupCrypto.decrypt(data, password)
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val name = e.name
                // Samo ocekivane putanje, bez "../" i podfoldera.
                val ok = name == DB_ENTRY || listOf(PHOTOS, EX_IMAGES).any { d ->
                    name.startsWith("$d/") && name.substringAfter("/").let { it.isNotEmpty() && '/' !in it && ".." !in it }
                }
                if (ok) entries[name] = zip.readBytes()
            }
        }
        val dbBytes = entries[DB_ENTRY] ?: throw BackupException("Kopija nema bazu podataka.")
        if (!String(dbBytes.copyOfRange(0, 15)).startsWith("SQLite format 3")) throw BackupException("Baza u kopiji je oštećena.")

        TreningApp.instance.db.close()
        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        dbFile.writeBytes(dbBytes)

        val photoDir = File(context.filesDir, PHOTOS).apply { deleteRecursively(); mkdirs() }
        val exDir = File(context.filesDir, EX_IMAGES).apply { mkdirs() }
        for ((name, bytes) in entries) when {
            name.startsWith("$PHOTOS/") -> File(photoDir, name.substringAfter("/")).writeBytes(SecurePhotoStore.encrypt(bytes))
            name.startsWith("$EX_IMAGES/") -> File(exDir, name.substringAfter("/")).writeBytes(bytes)
        }
        fixPaths(dbFile, photoDir, exDir)
    }

    /** Putanje u bazi pokazuju na stari telefon - preusmeri ih na fajlove ovde. */
    private fun fixPaths(dbFile: File, photoDir: File, exDir: File) {
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.rawQuery("SELECT id, localPath FROM body_photo", null).use { c ->
                while (c.moveToNext()) db.execSQL(
                    "UPDATE body_photo SET localPath = ? WHERE id = ?",
                    arrayOf(File(photoDir, File(c.getString(1)).name).path, c.getString(0)),
                )
            }
            db.rawQuery("SELECT id, customImage FROM exercise WHERE customImage IS NOT NULL", null).use { c ->
                while (c.moveToNext()) {
                    val file = File(exDir, Uri.parse(c.getString(1)).lastPathSegment ?: continue)
                    db.execSQL("UPDATE exercise SET customImage = ? WHERE id = ?", arrayOf(Uri.fromFile(file).toString(), c.getString(0)))
                }
            }
        }
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
