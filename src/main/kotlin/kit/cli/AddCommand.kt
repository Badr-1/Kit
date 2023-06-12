package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kit.porcelain.add

class AddCommand : CliktCommand(name = "add", help = "Add file contents to the index") {
    private val path by argument(
        help = "The path of the file to add",
        name = "path"
    )

    override fun run() {
        if (path.startsWith(System.getProperty("user.dir")))
            add(path)
        else
            add(System.getProperty("user.dir") + "/" + path)
    }
}