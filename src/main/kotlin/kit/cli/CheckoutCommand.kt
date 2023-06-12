package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kit.porcelain.checkout

class CheckoutCommand : CliktCommand(name = "checkout", help = "Switch branches or restore working tree files") {
    private val branchOrCommit by argument(
        help = "The branch or commit to checkout",
        name = "branchOrCommit"
    )

    override fun run() {
        checkout(branchOrCommit)
    }
}