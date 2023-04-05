package kit.plumbing

import kit.porcelain.Config
import kit.utils.*
import java.io.*
import java.security.MessageDigest


/**
 * @param path the path of the file to be added to the tree
 * @param option the option to use (either -a or -d)
 * @return success message
 */
fun updateIndex(path: String, option: String, sha1: String = "", cacheInfo: String = "") {
    when {
        option == "-d" -> {
            GitIndex.remove(File(path))
            println("Removed ${File(path).relativePath().red()} from index")
        }

        option == "-a" && cacheInfo.isNotEmpty() -> {
            GitIndex.add(File(path), sha1, cacheInfo)
            println("Added ${File(path).relativePath().green()} to index")
        }

        else -> {
            throw IllegalArgumentException("usage: update-index <path> (-a|-d) <sha1> <cacheInfo>")
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
        "author ${Config.get("user.name")} <${Config.get("user.email")}> ${System.currentTimeMillis() / 1000} +0200\n"
    val committer =
        "committer ${Config.get("user.name")} <${Config.get("user.email")}> ${System.currentTimeMillis() / 1000} +0200\n\n"
    val message = commitMessage.ifEmpty { throw Exception("commit message is empty") } + "\n"
    val content = tree + parent + author + committer + message
    return hashObject(content, type = "commit", write = true)
}

/**
 * display the content of an object
 * @param hashObject the hash of the object
 * @param option the option to use (either -t, -s or -p)
 * @return the content of the object
 */
fun catFile(hashObject: String, option: String): String {
    val workingDirectory = System.getProperty("user.dir")
    val path = "${workingDirectory}/.kit/objects/${hashObject.substring(0, 2)}/${hashObject.substring(2)}"
    val content = Zlib.inflate(File(path).readBytes())
    val contentStr = content.toString(Charsets.UTF_8)
    val type = contentStr.substringBefore(" ")
    val size = contentStr.substringAfter(" ").substringBefore("\u0000").toInt()
    val contentWithoutHeader = contentStr.substringAfter("\u0000")

    /**
     * content without the header depends on the type of the object
     * - object: just use UTF-8 encoding
     * - commit: just use UTF-8 encoding
     * - tree: use the tree parser
     * */

    return when (option) {
        "-t" -> type.apply { println(this) }
        "-s" -> size.toString().apply { println(this) }
        "-p" -> {
            if (type == "tree") {
                val list = content.toMutableList()
                // remove till the first NUL
                list.subList(0, list.indexOf(0.toByte()) + 1).clear()
                parseTreeContent(list)
            } else
                contentWithoutHeader.apply { println(this) }
        }

        else -> throw IllegalArgumentException("usage: cat-file [-t | -s | -p] <object>")
    }
}


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
        // why not throw an exception?
        // because we want to run this recursively
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
            val indexEntry = GitIndex.get(file.relativePath())
                ?: // case 1
                continue

            /**
             * mode per git documentation:
             * @see <a href="https://git-scm.com/book/en/v2/Git-Internals-Git-Objects">git Objects - Tree Objects</a>
             */
            val mode = getMode(file)
            // case 2 & 3
            entries.add(TreeEntry(mode, file.name, indexEntry.sha1))
        }
    }
    return mkTree(
        entries,
        write
    ).apply {
        if (write) println(
            "writing object of type ${"tree".blue()} into the object database ${
                this.substring(
                    0,
                    7
                ).red()
            }"
        )
    }
}

/**
 * List the files in the index
 */
fun lsFiles(): String {
    return GitIndex.list().apply { println(this) }
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

    return sha1.apply {
        when {
            write && type == "blob" -> println(
                "writing " + File(path).relativePath()
                    .yellow() + " object of type " + "blob".blue() + " into the object database " + this.substring(0, 7)
                    .red()
            )

            write && type == "commit" -> println(
                "writing object of type " + "commit".blue() + " into the object database " + this.substring(
                    0,
                    7
                ).red()
            )
        }
    }
}


/********** helper functions **********/

/**
 * Hashes the given bytes using sha1
 * @param bytes the bytes to be hashed
 * @return the sha1 hash of the bytes
 */
fun sha1(bytes: ByteArray): String {
    val digest = MessageDigest
        .getInstance("SHA-1")
    digest.reset()
    digest.update(bytes)
    return digest.digest().joinToString("") { "%02x".format(it) }
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
 * data class to represent a tree entry
 * @param mode the mode of the entry
 * @param path the path of the entry
 * @param hash the hash of the entry
 */
data class TreeEntry(val mode: String, val path: String, val hash: String)


/**
 * helper function for catFile to parse the content of a tree
 * @param contentWithoutHeader the content of the tree without the header
 * @return the parsed content of the tree
 */
fun parseTreeContent(contentWithoutHeader: MutableList<Byte>): String {
    /**
     * format of entries in the tree:
     * mode SP path NUL sha1
     * mode: 6 bytes
     * SP: 1 byte
     * path: variable length
     * NUL: 1 byte
     * sha1: 20 bytes
     * parse till the end of the content
     * */
    val entries = mutableListOf<TreeEntry>()
    while (contentWithoutHeader.isNotEmpty()) {
        val mode = contentWithoutHeader.subList(0, contentWithoutHeader.indexOf(32.toByte())).toByteArray()
            .toString(Charsets.UTF_8)
        contentWithoutHeader.subList(0, contentWithoutHeader.indexOf(32.toByte())).clear()
        contentWithoutHeader.removeAt(0) // remove the space
        val path = contentWithoutHeader.subList(0, contentWithoutHeader.indexOf(0.toByte())).toByteArray()
            .toString(Charsets.UTF_8)
        contentWithoutHeader.subList(0, contentWithoutHeader.indexOf(0.toByte()) + 1).clear()
        val sha1 = contentWithoutHeader.subList(0, 20).joinToString("") { "%02x".format(it) }
        contentWithoutHeader.subList(0, 20).clear()
        entries.add(TreeEntry(mode, path, sha1))
    }
    return entries.joinToString("\n") {
        "${it.mode} ${getType(it.hash)} ${it.hash}\t${it.path}"
    }.apply {
        entries.joinToString("\n") {
            "${it.mode.green()} ${getType(it.hash).blue()} ${it.hash.red()}\t${it.path.yellow()}"
        }.apply {
            println(this)
        }
    }
}


/**
 * gets the path of the object in the object database
 * @receiver the hash of the object
 * @return the path of the object in the object database
 */
fun String.objectPath(): String {
    return "${System.getProperty("user.dir")}/.kit/objects/${this.substring(0, 2)}/${this.substring(2)}"
}

/**
 * check if an object exists in the object database
 * @receiver the hash of the object
 * @return true if the object exists, false otherwise
 */
fun String.objectExists(): Boolean {
    return File(this.objectPath()).exists()
}

fun getType(hashObject: String): String {
    val workingDirectory = System.getProperty("user.dir")
    val path = "${workingDirectory}/.kit/objects/${hashObject.substring(0, 2)}/${hashObject.substring(2)}"
    val content = Zlib.inflate(File(path).readBytes())
    val contentStr = content.toString(Charsets.UTF_8)
    return contentStr.substringBefore(" ")
}

fun getContent(hashObject: String): String {
    val workingDirectory = System.getProperty("user.dir")
    val path = "${workingDirectory}/.kit/objects/${hashObject.substring(0, 2)}/${hashObject.substring(2)}"
    val content = Zlib.inflate(File(path).readBytes())
    val contentStr = content.toString(Charsets.UTF_8)
    val type = contentStr.substringBefore(" ")
    val contentWithoutHeader = contentStr.substringAfter("\u0000")


    return if (type == "tree") {
        val list = content.toMutableList()
        // remove till the first NUL
        list.subList(0, list.indexOf(0.toByte()) + 1).clear()
        parseTreeContent(list)
    } else
        contentWithoutHeader
}

/********** helper functions **********/