package kit.plumbing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kit.plumbing.GitIndex.clearIndex
import kit.plumbing.GitIndex.readIndex
import kit.utils.*
import java.io.File
import kotlin.random.Random
import kotlin.test.fail

class PlumbingKtTest {

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        // clean up
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.deleteRecursively()
    }

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
        val kHash = hashObject(file.path)
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
            hashObject(file.path)
            fail("The hashObject function should throw an exception")
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
        System.setProperty("user.dir", workingDirectory.path)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        val kHash = hashObject(file.path, write = true)
        // check if the file was written to the object database
        val objectFile =
            File("src/test/resources/workingDirectory/.kit/objects/${kHash.substring(0, 2)}/${kHash.substring(2)}")
        assertTrue(objectFile.exists())
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
        System.setProperty("user.dir", workingDirectory.path)
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        try {
            hashObject(file.path, write = true)
        } catch (e: Exception) {
            assertEquals(
                /* expected = */ "The repository doesn't exist",
                /* actual = */ e.message,
                /* message = */ "The exception message should be 'The repository doesn't exist'"
            )
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
        System.setProperty("user.dir", workingDirectory.path)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        val kHash = hashObject(file.path, write = true)
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
        System.setProperty("user.dir", workingDirectory.path)
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
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")

        // check if the file was written to the index
        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 1)

        // clean up
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
        System.setProperty("user.dir", workingDirectory.path)
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
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")

        // change file content
        file.writeText("Hello World (2)")
        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")


        // check if the file was written to the index
        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 1)
    }

    /**
     * Testing whether the updateIndex function removes the file from the index
     */
    @Test
    fun `updateIndex remove file`() {
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")

        // check if the file was written to the index
        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 1)

        // remove the file
        updateIndex(file.path, "-d")
        assert(GitIndex.getEntryCount() == 0)
        clearIndex()
    }

    /**
     * Testing whether the updateIndex function removes the selected file without removing the other files
     */
    @Test
    fun `updateIndex remove file leave the rest`() {
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        updateIndex(file2.path, "-a", hashObject(file2.path, write = true), "100644")

        // check if the file was written to the index
        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 2)

        // update index remove file
        updateIndex(file.path, "-d")
        assert(GitIndex.getEntryCount() == 1)

        // update index remove file2
        updateIndex(file2.path, "-d")
        assert(GitIndex.getEntryCount() == 0)
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
        System.setProperty("user.dir", workingDirectory.path)
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
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")

        // check if the file was written to the index
        readIndex() // this is a refresh of the index

        val indexFile = File("src/test/resources/workingDirectory/.kit/index")
        assertTrue(indexFile.exists())
        assert(GitIndex.getEntryCount() == 1)
    }

    /**
     * Testing invalid option with updateIndex
     */
    @Test
    fun `updateIndex invalid option`() {
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        val exception = assertThrows<IllegalArgumentException> {
            updateIndex(file.path, "-w", hashObject(file.path, write = true), "100644")
        }
        assertEquals("usage: update-index <path> (-a|-d) <sha1> <cacheInfo>", exception.message)
    }

    /**
     * Testing list files empty index
     */
    @Test
    fun `ls-files`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        // list files
        val output = lsFiles()
        assertEquals("", output)

        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")

        // list files
        val output2 = lsFiles()
        assertEquals("test.txt", output2)

        // clean up
        clearIndex()

    }


    /**
     * Testing write-tree with non-existing directory
     */
    @Test
    fun `write-tree directory doesn't exists`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        try {
            // write tree
            writeTree("nonExistingDirectory")
            // clean up
            clearIndex()
            fail("Should have thrown an exception")
        } catch (e: Exception) {
            assertEquals(
                "java.io.FileNotFoundException: nonExistingDirectory (No such file or directory)",
                e.toString()
            )
            // clean up
            clearIndex()
        }
    }

    /**
     * Testing write-tree with empty directory
     * @return empty string
     */
    @Test
    fun `write-tree directory is empty`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        // write tree
        val sha1 = writeTree("src/test/resources/workingDirectory")

        assertEquals("", sha1) // empty directory
    }

    /**
     * Testing write-tree sha1 against git sha1
     */
    @Test
    fun `write-tree with write = false`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        var sha1 = hashObject(file.path, write = true)
        assertEquals("5e1c309dae7f45e0f39b1bf3ac3cd9db12e7d689", sha1) //
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // write tree
        sha1 = writeTree("src/test/resources/workingDirectory", false)

        assertEquals("4f11af3e4e067fc319abd053205f39bc40652f05", sha1) // this sha1 is generated by git
        // clean up
        clearIndex()
    }

    /**
     * Testing write-tree that writes to the object database
     */
    @Test
    fun `write-tree with write = true`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        var sha1 = hashObject(file.path, write = true)
        assertEquals("5e1c309dae7f45e0f39b1bf3ac3cd9db12e7d689", sha1) //
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // write tree
        sha1 = writeTree("src/test/resources/workingDirectory", true)

        assertEquals("4f11af3e4e067fc319abd053205f39bc40652f05", sha1) // this sha1 is generated by git
        assert(
            File(
                "src/test/resources/workingDirectory/.kit/objects/" + sha1.substring(
                    0,
                    2
                ) + "/" + sha1.substring(2)
            ).exists()
        ) { "File does not exist" }
        // clean up
        clearIndex()
    }

    @Test
    fun `write-tree with write but object database doesn't exist`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        try {
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
            updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
            objectDatabase.deleteRecursively() // to simulate the object database doesn't exist
            writeTree("src/test/resources/workingDirectory", true)
        } catch (e: Exception) {
            assertEquals(
                /* expected = */ "The repository doesn't exist",
                /* actual = */ e.message,
                /* message = */ "The exception message should be 'The repository doesn't exist'"
            )
        }
    }


    @Test
    fun `write-tree ignore files that aren't in the index`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        val file2 = File("src/test/resources/workingDirectory/test2.txt")
        file2.createNewFile()
        file2.writeText("Hello World 2")
        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // ignore file2
        // write tree
        val sha1 = writeTree("src/test/resources/workingDirectory", true)
        assertEquals("4f11af3e4e067fc319abd053205f39bc40652f05", sha1) // this sha1 is generated by git

        // clean up
        clearIndex()
    }

    @Test
    fun `write-tree work with subdirectories`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        // create a dummy directory
        val dummyDirectory = File("src/test/resources/workingDirectory/dummy")
        dummyDirectory.mkdir()
        // create a file with the content "Hello World"
        val file = File("src/test/resources/workingDirectory/dummy/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // ignore file2
        // write tree
        val sha1 = writeTree("src/test/resources/workingDirectory", true)
        assertEquals("7c3ab9742549cc307a65f3c20b9aa488507a10da", sha1) // this sha1 is generated by git
    }

    @Test
    fun `commit-tree invalid tree`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        val randomSha1 = Random.nextBytes(20).joinToString { "%02x".format(it) }

        try {
            commitTree(randomSha1, "master")
            fail("The commit-tree should fail because the tree doesn't exist")
        } catch (e: Exception) {
            assertEquals(
                /* expected = */ "The tree doesn't exist",
                /* actual = */ e.message,
                /* message = */ "The exception message should be 'The tree doesn't exist'"
            )
        }
    }

    @Test
    fun `commit-tree invalid parent`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // write tree
        val sha1 = writeTree("src/test/resources/workingDirectory", true)
        val randomSha1 = Random.nextBytes(20).joinToString { "%02x".format(it) }

        try {
            commitTree(sha1, "init", randomSha1)
            fail("The commit-tree should fail because the tree doesn't exist")
        } catch (e: Exception) {
            assertEquals(
                /* expected = */ "parent commit doesn't exist",
                /* actual = */ e.message,
                /* message = */ "The exception message should be 'The tree doesn't exist'"
            )
            // clean up
            clearIndex()
        }
    }

    @Test
    fun `commit-tree valid commit`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        // create a file
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // write tree
        val tree = writeTree("src/test/resources/workingDirectory", true)
        // commit tree
        val commit = commitTree(tree, "test commit")
        assert(commit.objectExists())
        // due to the fact that the commit hash always changes, we can't check the hash
        // we check the content of the commit
        val commitContent =
            Zlib.inflate(File(commit.objectPath()).readBytes()).toString(Charsets.UTF_8).substringAfter("\u0000")
        assertEquals("tree $tree", commitContent.split("\n")[0])
        assertEquals("test commit", commitContent.split("\n")[4])

        // clean up
        clearIndex()
    }

    @Test
    fun `commit-tree valid commit with parent`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        // create a file
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // write tree
        val tree = writeTree("src/test/resources/workingDirectory", true)
        // commit tree
        val commit = commitTree(tree, "test commit")
        assert(commit.objectExists())
        // change the file
        file.writeText("Hello World 2")
        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // write tree
        val tree2 = writeTree("src/test/resources/workingDirectory", true)
        // commit tree
        val commit2 = commitTree(tree2, "test commit 2", commit)
        assert(commit2.objectExists())
    }

    @Test
    fun `cat-file blob`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create a file
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        val sha1 = hashObject(file.path, write = true)
        // cat the file
        val content = catFile(sha1, "-p")
        assertEquals("Hello World", content)
        val size = catFile(sha1, "-s")
        assertEquals("11", size)
        val type = catFile(sha1, "-t")
        assertEquals("blob", type)
    }

    @Test
    fun `cat-file tree`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create a file
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // write tree
        val sha1 = writeTree("src/test/resources/workingDirectory", true)
        // cat the file
        val content = catFile(sha1, "-p")
        assertEquals("100644 blob 5e1c309dae7f45e0f39b1bf3ac3cd9db12e7d689\ttest.txt", content)
        val size = catFile(sha1, "-s")
        assertEquals("36", size)
        val type = catFile(sha1, "-t")
        assertEquals("tree", type)

        // clean up
        clearIndex()
    }

    @Test
    fun `cat-file commit`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
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
        // create a file
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // update the index
        updateIndex(file.path, "-a", hashObject(file.path, write = true), "100644")
        // write tree
        val tree = writeTree("src/test/resources/workingDirectory", true)
        // commit tree
        val commit = commitTree(tree, "test commit")
        assert(commit.objectExists())
        // cat the file
        val content = catFile(commit, "-p")
        assertEquals(6, content.split("\n").size)
        val size = catFile(commit, "-s")
        assertEquals("152", size)
        val type = catFile(commit, "-t")
        assertEquals("commit", type)

        // clean up
        clearIndex()
    }

    @Test
    fun `cat-file invalid option`() {
        // create a working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        // create object database
        val objectDatabase = File("src/test/resources/workingDirectory/.kit/objects")
        objectDatabase.mkdirs()
        // create a file
        val file = File("src/test/resources/workingDirectory/test.txt")
        file.createNewFile()
        file.writeText("Hello World")
        // hash the file
        val sha1 = hashObject(file.path, write = true)
        // cat the file
        val exception = assertThrows<IllegalArgumentException> {
            catFile(sha1, "-x")
        }
        assertEquals("usage: cat-file [-t | -s | -p] <object>", exception.message)
    }
}