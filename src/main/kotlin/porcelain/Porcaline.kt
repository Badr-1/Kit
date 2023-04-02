package porcelain

import plumbing.GitIndex
import plumbing.hashObject
import plumbing.updateIndex
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
    if ((!File(filePath).exists()) || GitIndex.get(File(filePath).relativeTo(File(System.getProperty("user.dir"))).path) == null) {
        throw Exception("fatal: pathspec '$filePath' did not match any files")
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
    updateIndex(filePath, "-d", hashObject(filePath, write = true), mode)
}