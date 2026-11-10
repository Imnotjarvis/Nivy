package com.nivy.data.file

import android.net.Uri
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.junit.Test

class FileSystemTest {
    private val uri = mockk<Uri>()

    // A provider that gives no file for the uri: the document is gone, or the provider refuses.
    private val fileSystem = FileSystem(openFile = { _, _ -> null })

    @Test
    fun `a write the provider gives no file for fails, as not found`() {
        fileSystem.writeToFile(uri, "data").leftOrNull().shouldBeInstanceOf<FileSystem.Failure.FileNotFound>()
    }

    @Test
    fun `a read the provider gives no file for fails, as not found`() {
        fileSystem.read(uri).leftOrNull().shouldBeInstanceOf<FileSystem.Failure.FileNotFound>()
    }
}
