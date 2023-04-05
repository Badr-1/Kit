package utils
import java.io.File
import java.util.concurrent.TimeUnit

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
