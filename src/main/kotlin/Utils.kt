package utils
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

/**
 * Runs the given command and returns the output
 */
fun String.runCommand(): String {

    val process = ProcessBuilder(*split(" ").toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start().apply { waitFor(60, TimeUnit.MINUTES) }

    val output = process.inputStream.bufferedReader().readText()

    if (process.exitValue() == 0) {
        return output.trim()
    } else {
        throw RuntimeException("Command '$this' failed")
    }
}

/**
 * convert a hex string to a byte array
 */
fun String.hexStringToByteArray(): ByteArray {
    val len = length
    require(len % 2 == 0) { "Hex string must have even number of characters" }
    val byteArray = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        byteArray[i / 2] = ((Character.digit(get(i), 16) shl 4)
                + Character.digit(get(i + 1), 16)).toByte()
        i += 2
    }
    return byteArray
}

/**
 * returns the relative path of this file to the given path
 * @param path the path to which the relative path is calculated
 */
fun File.relativePath(path: String = System.getProperty("user.dir")): String = this.relativeTo(File(path)).path

/**
 * helper function that returns the mode of a file
 * @param file the file
 * @return the mode based on git's documentation
 */
fun getMode(file: File): String {
    val mode = when {
        // check if the file is executable
        file.canExecute() -> "100755"
        // check if the file is a symlink
        Files.isSymbolicLink(Path(file.path)) -> "120000"
        // then it's a normal file
        else -> "100644"
    }
    return mode
}

/**
 * colorize the output in blue
 */
fun String.red() = "\u001B[31m$this\u001B[0m"

/**
 * colorize the output in green
 */
fun String.green() = "\u001B[32m$this\u001B[0m"

/**
 * colorize the output in yellow
 */
fun String.yellow() = "\u001B[33m$this\u001B[0m"