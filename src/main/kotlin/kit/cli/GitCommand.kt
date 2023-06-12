package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File

class GitCommand : CliktCommand(name = "git", help = "convert kit repository to git repository") {
    override fun run() {
        // change .kit to .git
        val kitDir = File("${System.getProperty("user.dir")}/.kit")
        val gitDir = File("${System.getProperty("user.dir")}/.git")
        kitDir.renameTo(gitDir)
    }
}