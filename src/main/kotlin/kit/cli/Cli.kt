package kit.cli

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.subcommands

object Cli{
    val kit = Kit().subcommands(
        InitCommand(),
        ConfigCommand(),
        GitCommand(),
        AddCommand(),
        UnStageCommand(),
        StatusCommand(),
        CommitCommand(),
        LogCommand(),
        CheckoutCommand(),
        BranchCommand(),
        TagCommand(),
        CompletionCommand()
    )
}