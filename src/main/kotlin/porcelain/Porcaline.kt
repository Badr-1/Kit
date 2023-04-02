package porcelain

import plumbing.*
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path


fun init(repositoryName: String = ""): String {
    var path = "${System.getProperty("user.dir")}/"
    if (repositoryName.isNotEmpty()) {
        File("$path/$repositoryName").mkdir()
        path += "$repositoryName/"

        if (File("$path.kit").exists()) {
            return "Reinitialized existing Kit repository in ${File("${path}.kit").absolutePath}"
        }
    }
    // repository
    File("${path}.kit").mkdir()
    // objects database
    File("${path}.kit/objects").mkdir()
    // refs
    File("${path}.kit/refs").mkdir()
    File("${path}.kit/refs/heads").mkdir()
    // HEAD
    File("${path}.kit/HEAD").writeText("ref: refs/heads/master")
    // config
    // default config
    File("${path}.kit/config").writeText("[core]\n\trepositoryformatversion = 0\n\tfilemode = true\n\tbare = false\n\tlogallrefupdates = true\n")

    return "Initialized empty Kit repository in ${File("${path}.kit").absolutePath}"
}

fun add(filePath: String) {
    // check if the file exists
    if (!File(filePath).exists()) {
        throw Exception("fatal: pathspec '$filePath' did not match any files")
    }
    // check if the file is in the working directory
    if (!File(filePath).absolutePath.startsWith(File(System.getProperty("user.dir")).absolutePath)) {
        throw Exception("fatal: pathspec '$filePath' is outside repository")
    }
    // check if the file is in the .kit directory
    if (File(filePath).absolutePath.startsWith(File("${System.getProperty("user.dir")}/.kit").absolutePath)) {
        return
    }
    val file = File(filePath)
    // update the index
    val mode = when {
        // check if the file is executable
        file.canExecute() -> "100755"
        // check if the file is a symlink
        Files.isSymbolicLink(Path(file.path)) -> "120000"
        // then it's a normal file
        else -> "100644"
    }
    updateIndex(filePath, "-a", hashObject(filePath, write = true), mode)
}

fun unstage(filePath: String) {
    // check if the file is in the working directory
    if (!File(filePath).absolutePath.startsWith(File(System.getProperty("user.dir")).absolutePath)) {
        throw Exception("fatal: pathspec '$filePath' is outside repository")
    }
    // check if the file exists or is in the index
    if (GitIndex.get(File(filePath).relativeTo(File(System.getProperty("user.dir"))).path) == null) {
        throw Exception("fatal: pathspec '$filePath' did not match any files")
    }
    updateIndex(filePath, "-d")
}

// TODO this should be updated when the commit command is implemented
fun status(): String {
    // get all the files in the working directory except the .kit directory
    val workingDirectoryFiles = File(System.getProperty("user.dir")).walk()
        .filter { it.isFile && !it.path.contains(".kit") }.toList().map { it.relativePath() }

    // get all the files in the index
    val indexFiles = GitIndex.entries().map { it.path }

    // untracked files are the files that are in the working directory but not in the index
    val untrackedFiles = workingDirectoryFiles.filter { !indexFiles.contains(it) }

    // modified files are the files that are in the index but has different hash from the working directory
    val modifiedFiles = indexFiles.filter {
        workingDirectoryFiles.contains(it) && GitIndex.get(it)!!.sha1 != hashObject("${System.getProperty("user.dir")}/$it")
    }
    // deleted files are the files that are in the index but not in the working directory
    val deletedFiles = indexFiles.filter { !workingDirectoryFiles.contains(it) }

    // added files
    val addedFiles = indexFiles.filter { !deletedFiles.contains(it) && !modifiedFiles.contains(it) }

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

// TODO think about adding amend option
fun commit(message: String): String {
    val head = File("${System.getProperty("user.dir")}/.kit/HEAD").readText()
    val parent = when {
        head.contains("ref: refs/heads/") -> {
            val branch = File(System.getProperty("user.dir") + "/.kit/" + head.split(" ")[1])
            if (branch.exists()) {
                branch.readText()
            } else {
                ""
            }
        }

        else -> head // commit hash
    }
    val treeHash = writeTree(System.getProperty("user.dir"), write = true)
    val commitHash = commitTree(treeHash, message, parent)
    if (head.contains("ref: refs/heads/")) {
        val branch = File(System.getProperty("user.dir") + "/.kit/" + head.split(" ")[1])
        branch.createNewFile()
        branch.writeText(commitHash)
    } else {
        File("${System.getProperty("user.dir")}/.kit/HEAD").writeText(commitHash)
    }
    return commitHash
}

fun File.relativePath(): String = this.relativeTo(File(System.getProperty("user.dir"))).path

fun String.red() = "\u001B[31m$this\u001B[0m"
fun String.green() = "\u001B[32m$this\u001B[0m"
fun String.yellow() = "\u001B[33m$this\u001B[0m"