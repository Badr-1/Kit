package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import kit.plumbing.GitIndex
import kit.porcelain.Config
import java.io.File
import kotlin.system.exitProcess

class Kit : CliktCommand(name = "kit", help = "The kit version control system") {
    override fun run() {
        val context = currentContext
        val subcommand = context.invokedSubcommand
        if (File("${System.getProperty("user.dir")}/.kit").exists()) {
            // load config file
            Config.read()
            // load index file
            GitIndex
        } else {
           // only the init command is allowed to run without a .kit directory
            if (subcommand?.commandName != "init") {
                echo("Not a kit repository (or any of the parent directories): .kit")
                exitProcess(1)
            }
        }
    }
}