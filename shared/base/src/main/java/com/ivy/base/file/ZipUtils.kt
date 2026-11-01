package com.ivy.base.file

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val MODE_WRITE = "w"
private const val MODE_READ = "r"

fun zip(context: Context, zipFile: Uri, files: List<File>) {
    context.contentResolver.openFileDescriptor(zipFile, MODE_WRITE).use { descriptor ->
        descriptor?.fileDescriptor?.let {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(it))).use { outStream ->
                zip(outStream, files)
            }
        }
    }
}

private fun zip(
    outStream: ZipOutputStream,
    files: List<File>,
    includeParentFolder: Boolean = false
) {
    files.forEach { file ->
        if (file.isDirectory) {
            file.mkdir()
            zip(outStream, file.listFiles()?.toList() ?: emptyList(), includeParentFolder = true)
        } else {
            val fileLoc: String =
                if (file.parent.isNullOrEmpty() || !includeParentFolder) {
                    file.name
                } else {
                    (file.parent!!).substring(
                        file.parent!!.lastIndexOf("/")
                    ) + "/" + file.name
                }

            outStream.putNextEntry(ZipEntry(fileLoc))
            BufferedInputStream(FileInputStream(file)).use { inStream ->
                inStream.copyTo(outStream)
            }
        }
    }
}

fun unzip(context: Context, zipFile: Uri, location: File) {
    context.contentResolver.openFileDescriptor(zipFile, MODE_READ).use { descriptor ->
        descriptor?.fileDescriptor?.let {
            unzip(FileInputStream(it), location)
        }
    }
}

private const val BYTES_PER_MB = 1024L * 1024

/** A backup is a few megabytes; an archive inflating past this is not one and is rejected. */
const val MAX_UNZIPPED_BYTES: Long = 256 * BYTES_PER_MB

fun unzip(fileInputStream: FileInputStream, location: File, maxBytes: Long = MAX_UNZIPPED_BYTES) {
    ZipInputStream(BufferedInputStream(fileInputStream)).use { zipInputStream ->
        check(!location.exists() || location.isDirectory) {
            "Location file must be directory or not exist"
        }

        if (!location.isDirectory) location.mkdirs()

        val destination = location.canonicalFile.toPath()
        var written = 0L
        generateSequence { zipInputStream.nextEntry }.forEach { entry ->
            // An entry named like "../databases/x" or "/data/x" would be written outside the
            // destination folder ("Zip Slip"), so the archive is rejected instead.
            val target = destination.resolve(entry.name).normalize()
            if (target.startsWith(destination)) {
                written += extractEntry(zipInputStream, entry, target.toFile(), maxBytes - written)
            } else {
                error("Zip entry outside the destination folder: ${entry.name}")
            }
        }
    }
}

/** Writes the entry and returns its size; stops with an error once [limit] bytes are exceeded. */
private fun extractEntry(zipInputStream: ZipInputStream, entry: ZipEntry, file: File, limit: Long): Long {
    if (entry.isDirectory) {
        if (!file.isDirectory) file.mkdirs()
        return 0
    }
    file.parentFile?.takeIf { !it.isDirectory }?.mkdirs()
    var written = 0L
    BufferedOutputStream(FileOutputStream(file)).use { outStream ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = zipInputStream.read(buffer)
            if (read < 0) break
            written += read
            // The entry's declared size can lie; count what actually comes out.
            check(written <= limit) { "Zip inflates past the ${limit / BYTES_PER_MB} MB limit: ${entry.name}" }
            outStream.write(buffer, 0, read)
        }
    }
    return written
}
