package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import kit.porcelain.log

class LogCommand : CliktCommand(name = "log", help = "Show commit logs") {
    override fun run() {
        log()
    }
}