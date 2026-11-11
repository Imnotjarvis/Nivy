package com.nivy.data.file

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import arrow.core.Either
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.inject.Inject

class FileSystem internal constructor(
    // Opens the uri's file in a mode, as the content resolver does; tests give their own.
    private val openFile: (uri: Uri, mode: String) -> ParcelFileDescriptor?,
) {
    @Inject
    constructor(@ApplicationContext appContext: Context) :
        this({ uri, mode -> appContext.contentResolver.openFileDescriptor(uri, mode) })

    // Whatever fails, the caller gets a Failure rather than an exception, and the cause is logged.
    @Suppress("TooGenericExceptionCaught")
    fun writeToFile(uri: Uri, content: String): Either<Failure, Unit> = try {
        descriptor(uri, mode = "w").use {
            FileOutputStream(it.fileDescriptor).writer(charset = Charsets.UTF_16).use { writer ->
                writer.write(content)
            }
        }
        Either.Right(Unit)
    } catch (e: FileNotFoundException) {
        Timber.e(e, "No file to write")
        Either.Left(Failure.FileNotFound(e))
    } catch (e: Exception) {
        Timber.e(e, "Writing the file failed")
        Either.Left(Failure.IO(e))
    }

    // Whatever fails, the caller gets a Failure rather than an exception, and the cause is logged.
    @Suppress("TooGenericExceptionCaught")
    fun read(
        uri: Uri,
        charset: Charset = Charsets.UTF_8
    ): Either<Failure, String> = try {
        val content = descriptor(uri, mode = "r").use {
            FileInputStream(it.fileDescriptor).use { fileInputStream ->
                readFileContent(fileInputStream = fileInputStream, charset = charset)
            }
        }
        Either.Right(content)
    } catch (e: FileNotFoundException) {
        Timber.e(e, "No file to read")
        Either.Left(Failure.FileNotFound(e))
    } catch (e: Exception) {
        Timber.e(e, "Reading the file failed")
        Either.Left(Failure.IO(e))
    }

    /** Zips [files] into the uri's file. @throws IOException when it can't be written. */
    fun zip(uri: Uri, files: List<File>) = com.nivy.base.file.zip(open = { openFile(uri, "w") }, files)

    /** Unzips the uri's file into [location]. @throws IOException when it can't be read or isn't a backup's. */
    fun unzip(uri: Uri, location: File) = com.nivy.base.file.unzip(open = { openFile(uri, "r") }, location)

    /** Copies the uri's file into [file]. @throws IOException when it can't be read or written. */
    fun copy(uri: Uri, file: File) {
        descriptor(uri, mode = "r").use {
            FileInputStream(it.fileDescriptor).use { input -> FileOutputStream(file).use(input::copyTo) }
        }
    }

    /** Whether the uri's file starts as a zip archive does; false when it can't be read. */
    fun isZip(uri: Uri): Boolean = try {
        descriptor(uri, mode = "r").use {
            FileInputStream(it.fileDescriptor).use { input ->
                val start = ByteArray(ZipSignature.size)
                input.read(start) == start.size && start.contentEquals(ZipSignature)
            }
        }
    } catch (e: IOException) {
        Timber.w(e, "Couldn't read the file's start")
        false
    }

    // The provider can give no file at all for the uri: that's a file not found, not a success.
    private fun descriptor(uri: Uri, mode: String): ParcelFileDescriptor =
        openFile(uri, mode) ?: throw FileNotFoundException("The provider gave no file to open")

    @Throws(IOException::class)
    private fun readFileContent(
        fileInputStream: FileInputStream,
        charset: Charset
    ): String {
        BufferedReader(InputStreamReader(fileInputStream, charset)).use { br ->
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append('\n')
            }
            return sb.toString()
        }
    }

    private companion object {
        // Every zip archive starts with "PK".
        val ZipSignature = byteArrayOf(0x50, 0x4B)
    }

    sealed interface Failure {
        val e: Throwable

        data class FileNotFound(override val e: Throwable) : Failure
        data class IO(override val e: Throwable) : Failure
    }
}
