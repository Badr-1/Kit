package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import kit.porcelain.status

class StatusCommand : CliktCommand(name = "status", help = "Show the working tree status") {
    override fun run() {
        status()
    }
}