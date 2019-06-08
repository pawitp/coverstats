package coverstats.server.utils

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun ByteArray.gzip(): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).use { it.write(this) }
    return bos.toByteArray()
}

fun ByteArray.gunzip(): ByteArray =
    GZIPInputStream(inputStream()).use { it.readBytes() }

