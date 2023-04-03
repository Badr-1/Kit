package porcelain

import plumbing.*
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

object Config {
    /**
     * consists of the following:
     * - sections, eg [ core ]
     * - keys, eg repositoryformatversion
     * - values, eg 0
     * */
    private fun getConfigFile() = File("${System.getProperty("user.dir")}/.kit/config")
    private val sections = mutableListOf("core") // to be updated when more sections are added
    private val values = mutableMapOf(
        "core" to mutableMapOf(
            "repositoryformatversion" to "0",
            "filemode" to "true",
            "bare" to "false",
            "logallrefupdates" to "true"
        )
    )

    fun unset() {
        sections.removeIf { it != "core" }
    }

    fun write() {
        getConfigFile().createNewFile()
        getConfigFile().writeText("")
        for (section in sections) {
            getConfigFile().appendText("[$section]\n")
            for ((key, value) in values[section]!!) {
                getConfigFile().appendText("\t$key = $value\n")
            }
        }
    }

    fun set(sectionWithKey: String, value: String) {
        val section = sectionWithKey.split(".")[0]
        val key = sectionWithKey.split(".")[1]
        if (!sections.contains(section)) {
            sections.add(section)
            values[section] = mutableMapOf()
        }
        values[section]!![key] = value
        write()
    }

    fun get(sectionWithKey: String): String {
        val section = sectionWithKey.split(".")[0]
        val key = sectionWithKey.split(".")[1]
        if (section == "user" && (key == "name" || key == "email")) {
            if (!sections.contains(section)) {
                sections.add(section)
                values[section] = mutableMapOf()
            }
            if (values[section]!![key] == null) values[section]!![key] = "Kit $key"
        }
        return values[section]!![key]!!
    }
}


fun init(repositoryName: String = ""): String {
    var path = "${System.getProperty("user.dir")}/"
    if (repositoryName.isNotEmpty()) {
        File("$path/$repositoryName").mkdir()
        if (!path.contains(repositoryName)) {
            path += "$repositoryName/"
        }
        System.setProperty("user.dir", path)
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
    Config.unset()
    Config.write()
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

fun checkout(ref: String) {
    // ref could be a commit hash or a branch name
    val commitHash = when {
        // TODO think about supporting working with commit hashes less than 40 characters (short commit hashes)
        // TODO think about checking out files
        ref.matches(Regex("[0-9a-f]{40}")) -> ref
        else -> {
            val branch = File("${System.getProperty("user.dir")}/.kit/refs/heads/$ref")
            if (!branch.exists()) {
                throw Exception("error: pathspec '$ref' did not match any file(s) known to kit")
            }
            branch.readText()
        }
    }
    // change the HEAD
    File("${System.getProperty("user.dir")}/.kit/HEAD").writeText(commitHash)
    updateWorkingDirectory(commitHash)
}

// this is a helper function for the actual log command
fun getHistory(): List<String> {
    val commits = mutableListOf<String>()
    val head = getHead()
    var it = head
    do {
        commits.add(it)
        it = getParent(it)
    } while (it != "")
    return commits
}

fun getRefs(): MutableMap<String, String> {
    val refs = mutableMapOf<String, String>()
    val branches = File("${System.getProperty("user.dir")}/.kit/refs/heads").listFiles()!!
    for (branch in branches) {
        refs[branch.readText()] = branch.relativeTo(File("${System.getProperty("user.dir")}/.kit/refs/heads")).path
    }
    return refs
}

fun log() {
    val commits = getHistory()
    val branches = getRefs()
    val head = when {
        File("${System.getProperty("user.dir")}/.kit/HEAD").readText().matches(Regex("[0-9a-f]{40}")) -> getHead()
        else -> branches[getHead()]!!
    }
    for (commit in commits) {
        val commitContent = catFile(commit, "-p")
        val hasParent = if (commitContent.contains("parent")) 0 else 1
        val authorLine = commitContent.split("\n")[2 - hasParent].split(" ").toMutableList()
        authorLine.removeFirst()
        authorLine.removeLast()
        authorLine.removeLast()
        val author = authorLine.joinToString(" ").green()
        // TODO add timestamp
        val message = commitContent.split("\n")[4 - hasParent]
        val refs = branches.filter { it.key == commit }.map { it.value }.toMutableList()
        if (commit == head) {
            refs.add("HEAD")
        } else if (refs.contains(head)) {
            refs.remove(head)
            refs.add("HEAD -> $head")
        }
        println(
            "* ${
                commit.substring(
                    0,
                    7
                ).red()
            }${if (refs.isNotEmpty()) " (${refs.joinToString()})".yellow() else ""} $message <$author>"
        )
    }
}

fun getParent(commitHash: String): String {
    val content = catFile(commitHash, "-p")
    return if (content.contains("parent")) {
        content.split("\n")[1].split(" ")[1]
    } else {
        ""
    }
}

fun getHead(): String {
    val head = File("${System.getProperty("user.dir")}/.kit/HEAD").readText()
    return if (head.contains("ref: refs/heads/")) {
        val branch = File(System.getProperty("user.dir") + "/.kit/" + head.split(" ")[1])
        branch.readText()
    } else {
        head
    }
}

fun branch(branchName: String, ref: String = "HEAD") {
    val branch = File("${System.getProperty("user.dir")}/.kit/refs/heads/$branchName")
    if (branch.exists()) {
        throw Exception("fatal: A branch named '$branchName' already exists.")
    } else {
        if (branchName.contains("/")) {
            branch.parentFile.mkdirs()
        }
        branch.createNewFile()
    }
    when (ref) {
        "HEAD" -> {
            val head = getHead()
            branch.writeText(head)
        }

        else -> {
            branch.writeText(ref)
        }
    }

}

fun updateWorkingDirectory(commitHash: String) {
    // get the tree hash from the commit
    val treeHash = getTreeHash(commitHash)
    val treeEntries = getTreeEntries(treeHash)
    treeEntries.forEach {
        val file = File("${System.getProperty("user.dir")}/${it.path}")
        if (it.mode == "100755") {
            file.setExecutable(true)
        }
        file.createNewFile()
        file.writeText(catFile(it.hash, "-p"))
    }
}

fun getTreeHash(commitHash: String): String {
    val content = catFile(commitHash, "-p")
    return content.split("\n")[0].split(" ")[1]
}

fun getTreeEntries(treeHash: String): List<TreeEntry> {
    val content = catFile(treeHash, "-p")
    val treeEntries = mutableListOf<TreeEntry>()
    content.split("\n").map {
        val mode = it.split(" ")[0]
        val sha1 = it.split(" ")[2].split("\t")[0]
        val path = it.split("\t")[1]
        if (mode == "40000") {
            treeEntries.addAll(getTreeEntries(sha1).map { treeEntry ->
                TreeEntry(
                    treeEntry.mode,
                    "$path/${treeEntry.path}",
                    treeEntry.hash
                )
            })
        } else {
            treeEntries.add(TreeEntry(mode, path, sha1))
        }
    }
    return treeEntries
}

fun File.relativePath(): String = this.relativeTo(File(System.getProperty("user.dir"))).path

fun String.red() = "\u001B[31m$this\u001B[0m"
fun String.green() = "\u001B[32m$this\u001B[0m"
fun String.yellow() = "\u001B[33m$this\u001B[0m"