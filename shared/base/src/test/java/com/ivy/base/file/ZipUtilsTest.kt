package com.ivy.base.file

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipUtilsTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `extracts every entry into the destination folder`() {
        // Given
        val zip = zipOf("backup.json" to "{}", "nested/notes.txt" to "hello")
        val destination = File(temp.root, "out")

        // When
        unzip(FileInputStream(zip), destination)

        // Then
        File(destination, "backup.json").readText() shouldBe "{}"
        File(destination, "nested/notes.txt").readText() shouldBe "hello"
    }

    @Test
    fun `rejects an archive that inflates past the limit`() {
        // Given: 2 MB of zeros, which zip to a few KB
        val zip = zipOf("backup.json" to "0".repeat(2 * 1024 * 1024))
        val destination = File(temp.root, "out")

        // When
        shouldThrow<IllegalStateException> {
            unzip(FileInputStream(zip), destination, maxBytes = 1024 * 1024)
        }

        // Then: nothing beyond the limit was written
        File(destination, "backup.json").length() shouldBeLessThanOrEqual 1024L * 1024
    }

    @Test
    fun `rejects entries that would be written outside the destination folder`() {
        listOf("../evil.txt", "nested/../../evil.txt", "${temp.root.absolutePath}/evil.txt")
            .forEach { name ->
                // Given
                val zip = zipOf(name to "pwned")
                val destination = File(temp.root, "out")

                // When
                shouldThrow<IllegalStateException> {
                    unzip(FileInputStream(zip), destination)
                }

                // Then
                File(temp.root, "evil.txt").exists() shouldBe false
            }
    }

    private fun zipOf(vararg entries: Pair<String, String>): File {
        val file = temp.newFile()
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }
}
