package porcelain

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
}