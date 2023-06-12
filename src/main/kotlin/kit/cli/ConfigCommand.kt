package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kit.porcelain.Config


class ConfigCommand : CliktCommand(name = "config", help = "Set kit configuration values") {
    private val name by argument(
        help = "The name of the configuration value",
        name = "name"
    )
    private val value by argument(
        help = "The value of the configuration value",
        name = "value"
    )

    override fun run() {
        Config.set(name, value)
    }
}