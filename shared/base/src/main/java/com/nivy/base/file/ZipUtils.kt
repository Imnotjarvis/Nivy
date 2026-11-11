package com.nivy.base.file

import android.os.ParcelFileDescriptor
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Zips [files], each a plain file under its own name, into the file [open] gives, opened for
 * writing, as a content resolver does.
 */
fun zip(open: () -> ParcelFileDescriptor?, files: List<File>) {
    descriptor(open).use { descriptor -> zip(FileOutputStream(descriptor.fileDescriptor), files) }
}

/** Zips [files] into [out], and closes it. */
internal fun zip(out: OutputStream, files: List<File>) {
    ZipOutputStream(BufferedOutputStream(out)).use { zipped ->
        files.forEach { file ->
            zipped.putNextEntry(ZipEntry(file.name))
            FileInputStream(file).use { it.copyTo(zipped) }
        }
    }
}

// The provider can give no file at all for the uri: that's a file not found, not a zip of nothing.
private fun descriptor(open: () -> ParcelFileDescriptor?): ParcelFileDescriptor =
    open() ?: throw FileNotFoundException("The provider gave no file to open")

/** Unzips the file [open] gives, opened for reading, as a content resolver does, into [location]. */
fun unzip(open: () -> ParcelFileDescriptor?, location: File) {
    descriptor(open).use { descriptor ->
        unzip(FileInputStream(descriptor.fileDescriptor), location)
    }
}

private const val BYTES_PER_MB = 1024L * 1024

/** A backup is a few megabytes; an archive inflating past this is not one and is rejected. */
private const val MAX_UNZIPPED_BYTES: Long = 256 * BYTES_PER_MB

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
