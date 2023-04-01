package plumbing

import java.io.File
import java.security.MessageDigest


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
 * Hashes the given file content using sha1
 * @param path the file path
 * @param write whether to write the hash to the object directory
 * @return the sha1 hash of the file
 */
fun hashObject(path: String, write: Boolean = false): String {
    // check if the file exists
    if (!File(path).exists()) {
        throw Exception("File does not exist")
    }
    val workingDirectory = System.getProperty("user.dir")
    // read the file content
    val content = File(path).readText()
    // encode the content
    val encoding = "UTF-8"
    val bytes = content.toByteArray(charset(encoding))
    // create the prefix as described in the git documentation
    /**
     * The prefix is a header that is prepended to the content before hashing.
     * It consists of the object type, a space, and the size of the content in bytes.
     * The header is followed by a NUL byte (0x00) and then the content.
     */
    val prefix = "blob ${bytes.size}\u0000"
    val sha1 = sha1(prefix.toByteArray(charset(encoding)) + bytes)
    if (write) {
        val objectPath = "${workingDirectory}/.kit/objects/${sha1.substring(0, 2)}/${sha1.substring(2)}"
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
