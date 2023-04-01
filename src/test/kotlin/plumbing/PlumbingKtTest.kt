package plumbing

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import plumbing.GitIndex.clearIndex
import plumbing.GitIndex.refreshIndex
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
            hashObject(file.absolutePath)
        } catch (e: Exception) {
            assertEquals(
                /* expected = */ "File does not exist",
                /* actual = */ e.message,
                /* message = */ "The exception message should be 'File does not exist'"
            )
        }
    }

    /**
     * Testing whether the hashObject function throws an exception when the file is a directory
     */
    @Test
    fun `hashObject file and write to object database`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        val kHash = hashObject(file.absolutePath, true)
        // check if the file was written to the object database
        val objectFile =
            File("src/test/resources/workingDirectory/.kit/objects/${kHash.substring(0, 2)}/${kHash.substring(2)}")
        assertTrue(objectFile.exists())

        // clean up
        workingDirectory.deleteRecursively()
    }

    /**
     * Testing whether the hashObject function throws an exception when the file is a directory
     */
    @Test
    fun `hashObject file and write but object database doesn't exist`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        try {
            hashObject(file.absolutePath, true)
        } catch (e: Exception) {
            assertEquals(
                /* expected = */ "The repository doesn't exist",
                /* actual = */ e.message,
                /* message = */ "The exception message should be 'The repository doesn't exist'"
            )
            // clean up
            workingDirectory.deleteRecursively()
        }
    }

    /**
     * Testing whether it match git functionality
     */
    @Test
    fun `hashObject file and write to object database and match git functionality`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        val kHash = hashObject(file.absolutePath, true)
        // hash the file using the command line
        val gHash = "git hash-object -w ${file.absolutePath}".runCommand()
        // NOTE: this affect the project repo due to the limitation that kotlin can't change the working directory

        // compare the content of the files
        val kObjectFile =
            File("src/test/resources/workingDirectory/.kit/objects/${kHash.substring(0, 2)}/${kHash.substring(2)}")
        val gObjectFile =
            File(".git/objects/${gHash.substring(0, 2)}/${gHash.substring(2)}")
        assertEquals(
            /* expected = */ Zlib.inflate(gObjectFile.readBytes()).toString(Charsets.UTF_8),
            /* actual = */ Zlib.inflate(kObjectFile.readBytes()).toString(Charsets.UTF_8),
            /* message = */ "The content of the files should be equal"
        )

        // clean up
        workingDirectory.deleteRecursively()
        gObjectFile.delete()
    }


    /**
     * Testing whether the updateIndex function adds the file to the index
     */
    @Test
    fun `updateIndex add file`() {
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create refs and refs/heads
        val refs = File("src/test/resources/workingDirectory/.kit/refs")
        refs.mkdirs()
        val heads = File("src/test/resources/workingDirectory/.kit/refs/heads")
        heads.mkdirs()
        // create HEAD file
        val head = File("src/test/resources/workingDirectory/.kit/HEAD")
        head.createNewFile()
        head.writeText("ref: refs/heads/master")
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.absolutePath, "-a", hashObject(file.absolutePath, true), "100644")

        // check if the file was written to the index
        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 1)

        // clean up
        workingDirectory.deleteRecursively()
        clearIndex()
    }

    /**
     * Testing whether the index update file that was already added after change
     */
    @Test
    fun `updateIndex add file that was already added after change`() {
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create refs and refs/heads
        val refs = File("src/test/resources/workingDirectory/.kit/refs")
        refs.mkdirs()
        val heads = File("src/test/resources/workingDirectory/.kit/refs/heads")
        heads.mkdirs()
        // create HEAD file
        val head = File("src/test/resources/workingDirectory/.kit/HEAD")
        head.createNewFile()
        head.writeText("ref: refs/heads/master")
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.absolutePath, "-a", hashObject(file.absolutePath, true), "100644")

        // change file content
        file.writeText("Hello World (2)")
        // update the index
        updateIndex(file.absolutePath, "-a", hashObject(file.absolutePath, true), "100644")


        // check if the file was written to the index
        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 1)

        // clean up
        workingDirectory.deleteRecursively()
    }

    /**
     * Testing whether the updateIndex function removes the file from the index
     */
    @Test
    fun `updateIndex remove file`() {
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create refs and refs/heads
        val refs = File("src/test/resources/workingDirectory/.kit/refs")
        refs.mkdirs()
        val heads = File("src/test/resources/workingDirectory/.kit/refs/heads")
        heads.mkdirs()
        // create HEAD file
        val head = File("src/test/resources/workingDirectory/.kit/HEAD")
        head.createNewFile()
        head.writeText("ref: refs/heads/master")
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.absolutePath, "-a", hashObject(file.absolutePath, true), "100644")

        // check if the file was written to the index
        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 1)

        // remove the file
        updateIndex(file.absolutePath, "-d")
        assert(GitIndex.getEntryCount() == 0)

        refreshIndex()

        // clean up
        workingDirectory.deleteRecursively()
    }

    /**
     * Testing whether the updateIndex function removes the selected file without removing the other files
     */
    @Test
    fun `updateIndex remove file leave the rest`() {
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create refs and refs/heads
        val refs = File("src/test/resources/workingDirectory/.kit/refs")
        refs.mkdirs()
        val heads = File("src/test/resources/workingDirectory/.kit/refs/heads")
        heads.mkdirs()
        // create HEAD file
        val head = File("src/test/resources/workingDirectory/.kit/HEAD")
        head.createNewFile()
        head.writeText("ref: refs/heads/master")
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        val file2 = File("src/test/resources/workingDirectory/test2.txt")
        file.createNewFile()
        file2.createNewFile()
        file.writeText("Hello World")
        file2.writeText("Hello World")
        // update the index
        updateIndex(file.absolutePath, "-a", hashObject(file.absolutePath, true), "100644")
        updateIndex(file2.absolutePath, "-a", hashObject(file2.absolutePath, true), "100644")

        // check if the file was written to the index
        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 2)

        // update index remove file
        updateIndex(file.absolutePath, "-d")
        assert(GitIndex.getEntryCount() == 1)

        // update index remove file2
        updateIndex(file2.absolutePath, "-d")
        assert(GitIndex.getEntryCount() == 0)

        // clean up
        workingDirectory.deleteRecursively()
    }

    /**
     * Testing whether the init of index works
     */
    @Test
    fun `loadIndex file`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create refs and refs/heads
        val refs = File("src/test/resources/workingDirectory/.kit/refs")
        refs.mkdirs()
        val heads = File("src/test/resources/workingDirectory/.kit/refs/heads")
        heads.mkdirs()
        // create HEAD file
        val head = File("src/test/resources/workingDirectory/.kit/HEAD")
        head.createNewFile()
        head.writeText("ref: refs/heads/master")
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.absolutePath, "-a", hashObject(file.absolutePath, true), "100644")

        // check if the file was written to the index
        refreshIndex() // this is a refresh of the index

        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 1)

        // clean up
        workingDirectory.deleteRecursively()

    }

    @Test
            /**
             * Testing invalid option with updateIndex
             */
    fun `updateIndex invalid option`() {
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.absolutePath)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create refs and refs/heads
        val refs = File("src/test/resources/workingDirectory/.kit/refs")
        refs.mkdirs()
        val heads = File("src/test/resources/workingDirectory/.kit/refs/heads")
        heads.mkdirs()
        // create HEAD file
        val head = File("src/test/resources/workingDirectory/.kit/HEAD")
        head.createNewFile()
        head.writeText("ref: refs/heads/master")
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        val output = updateIndex(file.absolutePath, "-w", hashObject(file.absolutePath, true), "100644")

        assertEquals("usage: update-index <path> (-a|-d) <sha1> <cacheInfo>", output)

        // clean up
        workingDirectory.deleteRecursively()
    }
}