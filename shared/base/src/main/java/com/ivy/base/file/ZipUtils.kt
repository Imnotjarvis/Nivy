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

fun unzip(fileInputStream: FileInputStream, location: File) {
    ZipInputStream(BufferedInputStream(fileInputStream)).use { zipInputStream ->
        check(!location.exists() || location.isDirectory) {
            "Location file must be directory or not exist"
        }

        if (!location.isDirectory) location.mkdirs()

        val destination = location.canonicalFile.toPath()
        generateSequence { zipInputStream.nextEntry }.forEach { entry ->
            // An entry named like "../databases/x" or "/data/x" would be written outside the
            // destination folder ("Zip Slip"), so the archive is rejected instead.
            val target = destination.resolve(entry.name).normalize()
            if (target.startsWith(destination)) {
                extractEntry(zipInputStream, entry, target.toFile())
            } else {
                error("Zip entry outside the destination folder: ${entry.name}")
            }
        }
    }
}

private fun extractEntry(zipInputStream: ZipInputStream, entry: ZipEntry, file: File) {
    if (entry.isDirectory) {
        if (!file.isDirectory) file.mkdirs()
    } else {
        file.parentFile?.takeIf { !it.isDirectory }?.mkdirs()
        BufferedOutputStream(FileOutputStream(file)).use { outStream ->
            zipInputStream.copyTo(outStream)
        }
    }
}
