package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kit.porcelain.branch

class BranchCommand : CliktCommand(name = "branch", help = "create branch") {
    private val branchName by argument(
        help = "The name of the branch",
        name = "branchName"
    )
    private val ref by argument().optional()

    override fun run() {
        if (ref == null)
            branch(branchName)
        else
            branch(branchName, ref!!)
    }
}
