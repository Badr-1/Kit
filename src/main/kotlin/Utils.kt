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