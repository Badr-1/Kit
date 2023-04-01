package plumbing

import hexStringToByteArray
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.path.Path


/**
 * @param path the path of the file to be added to the tree
 * @param option the option to use (either -a or -d)
 * @return success message
 */
fun updateIndex(path: String, option: String, sha1: String = "", cacheInfo: String = ""): String {
    return when {
        option == "-d" -> {
            GitIndex.remove(File(path))
            "Removed $path from index"
        }

        option == "-a" && cacheInfo.isNotEmpty() -> {
            GitIndex.add(File(path), sha1, cacheInfo)
            "Added $path to index"
        }

        else -> {
            "usage: update-index <path> (-a|-d) <sha1> <cacheInfo>"
        }
    }
}

/**
 * @param treeHash the hash of the tree to be committed
 * @param parentCommit the hash of the parent commit
 * @param commitMessage the message of the commit
 * @return the sha1 hash of the commit
 */
fun commitTree(treeHash: String, commitMessage: String, parentCommit: String = ""): String {
    // validate the tree
    if (!treeHash.objectExists()) {
        throw Exception("The tree doesn't exist")
    }
    // validate the parent commit
    if (parentCommit.isNotEmpty()) {
        if (!parentCommit.objectExists()) {
            throw Exception("parent commit doesn't exist")
        }
    }
    val tree = "tree $treeHash\n"
    val parent = if (parentCommit.isEmpty()) parentCommit else "parent $parentCommit\n"
    val author =
        "author user-name <user-email> ${System.currentTimeMillis() / 1000} +0200\n"
    val committer =
        "committer user-name <user-email> ${System.currentTimeMillis() / 1000} +0200\n"
    val message = commitMessage.ifEmpty { throw Exception("commit message is empty") }
    val content = tree + parent + author + committer + message
    return hashObject(content, type = "commit", write = true)
}

fun String.objectPath(): String {
    return "${System.getProperty("user.dir")}/.kit/objects/${this.substring(0, 2)}/${this.substring(2)}"
}

fun String.objectExists(): Boolean {
    return File(this.objectPath()).exists()
}

data class TreeEntry(val mode: String, val path: String, val hash: String)

/**
 * @param directory the directory to write the tree for
 * @param write whether to write the tree to the object database or not
 * @return the sha1 hash of the tree
 */
fun writeTree(directory: String, write: Boolean = false): String {
    val chosenDirectory = File(directory)
    if (!chosenDirectory.exists()) {
        throw FileNotFoundException("$directory (No such file or directory)")
    }
    if (chosenDirectory.listFiles()!!.toMutableList().none { it.name != ".kit" }) {
        println("directory $directory is empty") // TODO: remove this after debugging
        // why not throw an exception?
        // because we want to be to run this recursively
        // and there's no problem if a directory is empty just avoid it
        return ""
    }
    val files = chosenDirectory.listFiles()!!.toMutableList().filter { it.name != ".kit" }
    val entries = mutableListOf<TreeEntry>()
    for (file in files) {
        if (file.isDirectory) {
            val workingDirectory = System.getProperty("user.dir")
            System.setProperty("user.dir", file.parentFile.path)
            val treeHash = writeTree(file.path, write)
            if (treeHash.isNotEmpty())
                entries.add(TreeEntry("40000", file.name, treeHash))
            System.setProperty("user.dir", workingDirectory)
        } else {
            /**
             * there are three cases for files being added to the tree
             *
             * 1- the file is not in the index => ignore it, still untracked
             *
             * 2- the file is in the index with the same hash => add it to the tree, the file is either:
             *      1- not modified
             *      2- modified and staged
             *
             * 3- the file is in the index with a different hash => add what's in the index to the tree
             * this means that the file is modified but not staged yet.
             */
            val indexEntry = GitIndex.get(file.relativeTo(File(System.getProperty("user.dir"))).path)
            if (indexEntry == null) { // case 1
                println("file ${file.path} is not in the index") // TODO: remove this after debugging
                continue
            }
            /**
             * mode per git documentation:
             * @see <a href="https://git-scm.com/book/en/v2/Git-Internals-Git-Objects">git Objects - Tree Objects</a>
             */
            val mode = when {
                // check if the file is executable
                file.canExecute() -> "100755"
                // check if the file is a symlink
                Files.isSymbolicLink(Path(file.path)) -> "120000"
                // then it's a normal file
                else -> "100644"
            }
            // case 2 & 3
            entries.add(TreeEntry(mode, file.name, indexEntry.sha1))
        }
    }
    return mkTree(entries, write)
}

/**
 * @param entries the list of entries to be added to the tree
 * @param write whether to write the tree to the object database or not
 * @return the sha1 hash of the tree
 */
fun mkTree(entries: List<TreeEntry>, write: Boolean): String {
    var entriesContent: ByteArray = byteArrayOf()
    entries.forEach { entry ->
        val entryContent =
            entry.mode.toByteArray() + " ".toByteArray() + entry.path.toByteArray() + "\u0000".toByteArray() + entry.hash.hexStringToByteArray()
        entriesContent += entryContent
    }
    val prefix = "tree ${entriesContent.size}\u0000"
    val content = prefix.toByteArray(Charsets.UTF_8) + entriesContent
    val sha1 = sha1(content)
    if (write) {
        val workingDirectory = System.getProperty("user.dir")
        val path = sha1.objectPath()
        val objectDatabase = File("${workingDirectory}/.kit/objects")
        if (!objectDatabase.exists()) {
            // print files in the current directory
            throw Exception("The repository doesn't exist")
        } else {
            File(path).parentFile.mkdirs()
        }
        // compress the file content using zlib
        val compressed =
            Zlib.deflate(prefix.toByteArray() + entriesContent)
        // write the compressed content to the file
        File(path).writeBytes(compressed)
    }
    return sha1
}


/**
 * List the files in the index
 */
fun lsFiles(): String {
    return GitIndex.list()
}

/**
 * Hashes the given file content using sha1
 * @param path the file path
 * @param write whether to write the hash to the object directory
 * @return the sha1 hash of the file
 */
fun hashObject(path: String, type: String = "blob", write: Boolean = false): String {
    val workingDirectory = System.getProperty("user.dir")
    // read the file content
    val content = when (type) {
        "blob" -> {
            // check if the file exists
            if (!File(path).exists()) {
                throw Exception("File does not exist")
            }
            File(path).readText()
        }

        else -> path
    }
    // encode the content
    val encoding = "UTF-8"
    val bytes = content.toByteArray(charset(encoding))
    // create the prefix as described in the git documentation
    /**
     * The prefix is a header that is prepended to the content before hashing.
     * It consists of the object type, a space, and the size of the content in bytes.
     * The header is followed by a NUL byte (0x00) and then the content.
     */
    val prefix = "$type ${bytes.size}\u0000"
    val sha1 = sha1(prefix.toByteArray(charset(encoding)) + bytes)
    if (write) {
        val objectPath = sha1.objectPath()
        // make the directory if it doesn't exist
        val objectDatabase = File("${workingDirectory}/.kit/objects")
        if (!objectDatabase.exists()) {
            // print files in the current directory
            throw Exception("The repository doesn't exist")
        } else {
            File(objectPath).parentFile.mkdirs()
        }
        // compress the file content using zlib
        val compressed = Zlib.deflate(prefix.toByteArray() + content.toByteArray())
        // write the compressed content to the file
        File(objectPath).writeBytes(compressed)
    }

    return sha1
}

fun sha1(bytes: ByteArray): String {
    val digest = MessageDigest
        .getInstance("SHA-1")
    digest.reset()
    digest.update(bytes)
    return digest.digest().joinToString("") { "%02x".format(it) }
}
