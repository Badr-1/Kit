package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kit.porcelain.commit

class CommitCommand : CliktCommand(name = "commit", help = "Record changes to the repository") {
    private val message by option("-m", "--message", help = "Commit message").required()

    override fun run() {
        commit(message)
    }
}