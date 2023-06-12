package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kit.porcelain.unstage

class UnStageCommand : CliktCommand(name = "unstage", help = "Remove file contents from the index") {
    private val path by argument(
        help = "The path of the file to unstage",
        name = "path"
    )

    override fun run() {
        if (path.startsWith(System.getProperty("user.dir")))
            unstage(path)
        else
            unstage(System.getProperty("user.dir") + "/" + path)
    }
}