package porcelain

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import plumbing.GitIndex
import java.io.File

class PorcelainKtTest {

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        // clean up
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.deleteRecursively()
    }

    @Test
    fun `initialize a repository`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        assertEquals("Initialized empty Kit repository in ${File("${workingDirectory}/.kit").absolutePath}", init())
        assert(File("$workingDirectory/.kit").exists())
        assert(File("$workingDirectory/.kit/objects").exists())
        assert(File("$workingDirectory/.kit/refs").exists())
        assert(File("$workingDirectory/.kit/refs/heads").exists())
        assert(File("$workingDirectory/.kit/HEAD").exists())
        assert(File("$workingDirectory/.kit/HEAD").readText() == "ref: refs/heads/master")
        assert(File("$workingDirectory/.kit/config").exists())
    }

    @Test
    fun `initialize a repository with name`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        val repositoryName = "demo"
        assertEquals(
            "Initialized empty Kit repository in ${File("${workingDirectory}/$repositoryName/.kit").absolutePath}",
            init(repositoryName)
        )
        assert(File("$workingDirectory/$repositoryName/.kit").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/objects").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/refs").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/refs/heads").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/HEAD").exists())
        assert(File("$workingDirectory/$repositoryName/.kit/HEAD").readText() == "ref: refs/heads/master")
        assert(File("$workingDirectory/$repositoryName/.kit/config").exists())
    }

    @Test
    fun `initialize a repository with name in an existing repository`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        val repositoryName = "demo"
        assertEquals(
            "Initialized empty Kit repository in ${File("${workingDirectory}/$repositoryName/.kit").absolutePath}",
            init(repositoryName)
        )
        assertEquals(
            "Reinitialized existing Kit repository in ${File("${workingDirectory}/$repositoryName/.kit").absolutePath}",
            init(repositoryName)
        )
    }

    @Test
    fun `add non-existent file`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        val filePath = "src/test/resources/workingDirectory/non-existent-file"
        val exception = assertThrows<Exception> {
            add(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' did not match any files", exception.message)
    }

    @Test
    fun `add a file outside the repo`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        val filePath = "${workingDirectory.parent}/test.txt"
        File(filePath).writeText("test text")
        val exception = assertThrows<Exception> {
            add(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' is outside repository", exception.message)
        File(filePath).delete()
    }

    @Test
    fun `add file inside the kit directory`() {
        GitIndex.clearIndex()
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        val filePath = "${workingDirectory.path}/.kit/test.txt"
        File(filePath).writeText("test text")

        assertEquals(0, GitIndex.getEntryCount())
    }

    @Test
    fun `add a file`() {
        GitIndex.clearIndex()
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")

        assertEquals(0, GitIndex.getEntryCount())
        add(filePath)
        assertEquals(1, GitIndex.getEntryCount())
        // clean up
        GitIndex.clearIndex()
    }

    @Test
    fun `remove a file that's not in the index`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        val exception = assertThrows<Exception> {
            unstage(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' did not match any files", exception.message)
    }

    @Test
    fun `remove a file that's outside the repo`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        val filePath = "${workingDirectory.parent}/test.txt"
        File(filePath).writeText("test text")
        val exception = assertThrows<Exception> {
            unstage(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' is outside repository", exception.message)
        File(filePath).delete()
    }

    @Test
    fun `remove a file inside the kit directory`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        val filePath = "${workingDirectory.path}/.kit/test.txt"
        File(filePath).writeText("test text")
        val exception = assertThrows<Exception> {
            unstage(filePath)
        }
        assertEquals("fatal: pathspec '$filePath' did not match any files", exception.message)
        File(filePath).delete()
    }

    @Test
    fun `remove a file from index`() {
        GitIndex.clearIndex()
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        assertEquals(1, GitIndex.getEntryCount())
        unstage(filePath)
        assertEquals(0, GitIndex.getEntryCount())
    }
}