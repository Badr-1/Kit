package porcelain

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val filePath = "${workingDirectory.path}/.kit/test.txt"
        File(filePath).writeText("test text")

        assertEquals(0, GitIndex.getEntryCount())
    }

    @Test
    fun `add a file`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")

        assertEquals(0, GitIndex.getEntryCount())
        add(filePath)
        assertEquals(1, GitIndex.getEntryCount())
    }

    @Test
    fun `remove a file that's not in the index`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
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
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        assertEquals(1, GitIndex.getEntryCount())
        unstage(filePath)
        assertEquals(0, GitIndex.getEntryCount())
    }

    @Test
    fun `status all`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // array of files
        val files = arrayOf(
            "test.txt",
            "test2.txt",
            "test3.txt",
            "test4.txt"
        ).map { File("${workingDirectory.path}/$it") }.onEach { it.writeText("test text") }

        assertEquals(
            statusString(
                files.map { it.name },
                emptyList(),
                emptyList(),
                emptyList()
            ), status()
        )
        add(files[0].path)
        add(files[1].path)
        add(files[2].path)
        // change one file
        files[0].writeText("test text 2")
        // delete one file
        files[1].delete()

        assertEquals(
            statusString(
                listOf(files[3].relativePath()),
                listOf(files[2].relativePath()),
                listOf(files[0].relativePath()),
                listOf(files[1].relativePath())
            ), status()
        )

    }

    @Test
    fun `commit on master`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        // this should create a file in the .kit/refs/heads named master
        assertTrue(File("$workingDirectory/.kit/refs/heads/master").exists())
        assertEquals(commitHash, File("$workingDirectory/.kit/refs/heads/master").readText())
    }

    @Test
    fun `commit twice`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        File(filePath).writeText("test text 2")
        add(filePath)
        val commitHash2 = commit("test commit 2")
        assertEquals(commitHash2, File("$workingDirectory/.kit/refs/heads/master").readText())
    }

    @Test
    fun `commit when HEAD is at Detached state`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        // checkout to the commit
        File("$workingDirectory/.kit/HEAD").writeText(commitHash)
        File(filePath).writeText("test text 2")
        add(filePath)
        val commitHash2 = commit("test commit 2")

        assertEquals(commitHash2, File("$workingDirectory/.kit/HEAD").readText())
    }

    @Test
    fun `checkout non-existent branch`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        val exception = assertThrows<Exception> {
            checkout("test")
        }
        assertEquals("error: pathspec 'test' did not match any file(s) known to kit", exception.message)
    }

    @Test
    fun `checkout a commit`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        File(filePath).writeText("test text 2")
        add(filePath)
        commit("test commit 2")
        checkout(commitHash)
        assertEquals(commitHash, File("$workingDirectory/.kit/HEAD").readText())
        // the content of the file should be the same as the checkout commit
        assertEquals("test text", File(filePath).readText())
        log()
    }

    @Test
    fun `checkout a branch`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        File(filePath).writeText("test text 2")
        add(filePath)
        val commitHash = commit("test commit 2")
        checkout("master")
        assertEquals(commitHash, File("$workingDirectory/.kit/refs/heads/master").readText())
    }

    @Test
    fun `checkout an executable file`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.sh"
        File(filePath).writeText("echo test")
        File(filePath).setExecutable(true)
        add(filePath)
        val commitHash = commit("test commit")
        File(filePath).setExecutable(false)
        add(filePath)
        commit("test commit 2")
        checkout(commitHash)
        assertEquals(commitHash, File("$workingDirectory/.kit/HEAD").readText())
        // the content of the file should be the same as the checkout commit
        assertEquals("echo test", File(filePath).readText())
        assertTrue(File(filePath).canExecute())
    }

    @Test
    fun `checkout a directory`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test/test.txt"
        File(filePath).parentFile.mkdirs()
        File(filePath).writeText("test text")
        add(filePath)
        val commitHash = commit("test commit")
        File(filePath).writeText("test text 2")
        add(filePath)
        commit("test commit 2")
        checkout(commitHash)
        assertEquals(commitHash, File("$workingDirectory/.kit/HEAD").readText())
        // the content of the file should be the same as the checkout commit
        assertEquals("test text", File(filePath).readText())
    }

    @Test
    fun `make an existent branch`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        commit("test commit")
        val exception = assertThrows<Exception> {
            branch("master")
        }
        assertEquals("fatal: A branch named 'master' already exists.", exception.message)
    }

    @Test
    fun `make a new branch without directories`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        branch("test")
        assertEquals(hash, File("$workingDirectory/.kit/refs/heads/test").readText())

    }

    @Test
    fun `make a new branch with directories`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        branch("feature/test")
        assertEquals(hash, File("$workingDirectory/.kit/refs/heads/feature/test").readText())

    }

    @Test
    fun `make a new branch with a ref`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        branch("test", hash)
        assertEquals(hash, File("$workingDirectory/.kit/refs/heads/test").readText())

    }

    @Test
    fun `make a new branch without a ref`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        checkout(hash)
        branch("test")
        assertEquals(hash, File("$workingDirectory/.kit/refs/heads/test").readText())

    }


    @Test
    fun `set config`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        Config.set("user.name", "test")
        Config.set("user.email", "test@gmail.com")

        assertEquals("test", Config.get("user.name"))
        assertEquals("test@gmail.com", Config.get("user.email"))
    }

    @Test
    fun `get history one commit`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        // create a file
        val filePath = "${workingDirectory.path}/test.txt"
        File(filePath).writeText("test text")
        add(filePath)
        val hash = commit("test commit")
        val history = getHistory()
        assertEquals(hash, history[0])
    }

    @Test
    fun `get history multiple commit`() {
        // create working directory
        val workingDirectory = File("src/test/resources/workingDirectory")
        workingDirectory.mkdir()
        // set the working directory
        System.setProperty("user.dir", workingDirectory.path)
        init()
        if (GitIndex.getEntryCount() != 0) GitIndex.clearIndex()
        val commits = mutableListOf<String>()
        for (i in 1..5) {
            // create a file
            val filePath = "${workingDirectory.path}/test.txt"
            File(filePath).writeText("test text $i")
            add(filePath)
            val hash = commit("test commit")
            commits.add(hash)
            if (i == 3) {
                branch("demo")
            }
        }
        val history = getHistory()
        for (i in 0..4) {
            assertEquals(commits[4 - i], history[i])
        }
        log()
    }


    private fun statusString(
        untrackedFiles: List<String>,
        addedFiles: List<String>,
        modifiedFiles: List<String>,
        deletedFiles: List<String>
    ): String {
        return """
        On branch master
        
        Untracked files:
        ${untrackedFiles.sorted().joinToString("\n\t\t") { "?? $it".red() }}
        
        Changes to be committed :
        ${addedFiles.sorted().joinToString("\n\t\t") { "A $it".green() }}
        Changes not staged for commit:
        ${modifiedFiles.sorted().joinToString("\n\t\t") { "M $it".yellow() }}
        ${deletedFiles.sorted().joinToString("\n\t\t") { "D $it".yellow() }}

        """
    }
}
