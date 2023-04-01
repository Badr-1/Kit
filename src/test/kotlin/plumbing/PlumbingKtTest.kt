package plumbing

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import runCommand
import java.io.File

class PlumbingKtTest {


    /**
     * Testing whether the sha1 function returns the correct hash
     */
    @Test
    fun sha1() {
        val bytes = "Hello World".toByteArray()
        val hash = sha1(bytes)
        val command = "echo -n \"Hello World\" | shasum -a 1"
        val process = ProcessBuilder("/bin/bash", "-c", command)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        val cmdHash = process.inputStream.bufferedReader().readText().substring(0, 40)
        assertEquals(
            /* expected = */ cmdHash,
            /* actual = */ hash,
            /* message = */ "The hashes should be equal"
        )
    }

    /**
     * Testing whether the hashObject function returns the correct hash
     */
    @Test
    fun `hashObject file exists`() {
        // create a file with the content "Hello World"
        val file = File("src/test/resources/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        val kHash = hashObject(file.absolutePath)
        // hash the file using the command line
        val cmd = "git hash-object ${file.absolutePath}"
        val cmdHash = cmd.runCommand()
        file.delete()
        // compare the hashes
        assertEquals(
            /* expected = */ cmdHash,
            /* actual = */ kHash,
            /* message = */ "The hashes should be equal"
        )
    }

    /**
     * Testing whether the hashObject function throws an exception when the file does not exist
     */
    @Test
    fun `hashObject file does not exist`() {
        // create a file with the content "Hello World"
        val file = File("src/test/resources/test.txt")
        // hash the file
        try {
            val kHash = hashObject(file.absolutePath)
        } catch (e: Exception) {
            assertEquals(
                /* expected = */ "File does not exist",
                /* actual = */ e.message,
                /* message = */ "The exception message should be 'File does not exist'"
            )
        }
    }
}