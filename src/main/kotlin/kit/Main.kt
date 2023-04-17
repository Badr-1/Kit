package kit

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kit.plumbing.GitIndex
import kit.porcelain.*
import java.io.File
import kotlin.system.exitProcess

object Main {

    private fun checkIfKitRepo() {
        if (!File("${System.getProperty("user.dir")}/.kit").exists()) {
            println("Not a kit repository")
            exitProcess(1)
        }
    }

    class InitCommand : CliktCommand(name = "init", help = "Initialize a new, empty repository") {
        private val directory by argument(
            help = "Directory to initialize the repository in",
            name = "directory"
        ).optional()

        override fun run() {
            init(directory ?: "")
        }
    }

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
            checkIfKitRepo()
            Config.set(name, value)
        }
    }

    class GitCommand : CliktCommand(name = "git", help = "convert kit repository to git repository") {
        override fun run() {
            checkIfKitRepo()
            // change .kit to .git
            val kitDir = File("${System.getProperty("user.dir")}/.kit")
            val gitDir = File("${System.getProperty("user.dir")}/.git")
            kitDir.renameTo(gitDir)
        }
    }

    class AddCommand : CliktCommand(name = "add", help = "Add file contents to the index") {
        private val path by argument(
            help = "The path of the file to add",
            name = "path"
        )

        override fun run() {
            checkIfKitRepo()
            if (path.startsWith(System.getProperty("user.dir")))
                add(path)
            else
                add(System.getProperty("user.dir") + "/" + path)
        }
    }

    class UnStageCommand : CliktCommand(name = "unstage", help = "Remove file contents from the index") {
        private val path by argument(
            help = "The path of the file to unstage",
            name = "path"
        )

        override fun run() {
            checkIfKitRepo()
            if (path.startsWith(System.getProperty("user.dir")))
                unstage(path)
            else
                unstage(System.getProperty("user.dir") + "/" + path)
        }
    }

    class StatusCommand : CliktCommand(name = "status", help = "Show the working tree status") {
        override fun run() {
            checkIfKitRepo()
            status()
        }
    }

    class CommitCommand : CliktCommand(name = "commit", help = "Record changes to the repository") {
        private val message by option("-m", "--message", help = "Commit message").required()

        override fun run() {
            checkIfKitRepo()
            commit(message)
        }
    }

    class LogCommand : CliktCommand(name = "log", help = "Show commit logs") {
        override fun run() {
            checkIfKitRepo()
            log()
        }
    }

    class CheckoutCommand : CliktCommand(name = "checkout", help = "Switch branches or restore working tree files") {
        private val branchOrCommit by argument(
            help = "The branch or commit to checkout",
            name = "branchOrCommit"
        )

        override fun run() {
            checkIfKitRepo()
            checkout(branchOrCommit)
        }
    }

    class BranchCommand : CliktCommand(name = "branch", help = "create branch") {
        private val branchName by argument(
            help = "The name of the branch",
            name = "branchName"
        )
        private val ref by argument().optional()

        override fun run() {
            checkIfKitRepo()
            if (ref == null)
                branch(branchName)
            else
                branch(branchName, ref!!)
        }
    }

    class TagCommand : CliktCommand(name = "tag", help = "create tag") {
        private val tagName by argument(
            help = "The name of the tag",
            name = "tagName"
        )
        private val ref by argument().optional()
        private val message by option("-m", "--message", help = "Tag message").required()
        override fun run() {
            checkIfKitRepo()
            if (ref == null)
                tag(tagName, message)
            else
                tag(tagName, message, ref!!)
        }
    }

    class Kit : CliktCommand(name = "kit", help = "The kit version control system") {
        override fun run() {
            if (File("${System.getProperty("user.dir")}/.kit").exists()) {
                // load config file
                Config.read()
                // load index file
                GitIndex
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Kit().subcommands(
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
        ).main(args)
    }
}