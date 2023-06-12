package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kit.porcelain.init
import java.nio.file.Path

class InitCommand : CliktCommand(name = "init", help = "Initialize a new, empty repository") {
    private val directory by argument(
        help = "Directory to initialize the repository in",
        name = "directory"
    ).optional()

    override fun run() {
        val path = Path.of(directory ?: "").toAbsolutePath()
        init(path)
    }
}