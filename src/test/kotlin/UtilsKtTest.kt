import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import utils.runCommand

class UtilsKtTest {

    /**
     * Testing whether the runCommand function returns the correct output
     */
    @Test
    fun runCommand() {
        val command = "echo Hello World"
        val output = command.runCommand()
        assertEquals(
            /* expected = */ "Hello World",
            /* actual = */ output,
            /* message = */ "The output should be equal"
        )
    }
    /**
     * Testing whether it throws an exception when the command fails
     */
    @Test
    fun runCommandFail() {
        val command = "ls -l /non/existing/path"
        try {
            command.runCommand()
            fail("The command should fail")
        } catch (e: RuntimeException) {
            assertEquals(
                /* expected = */ "Command '$command' failed",
                /* actual = */ e.message,
                /* message = */ "The exception message should be equal"
            )
        }
    }
}