package kit

import kit.porcelain.*
import kit.porcelain.init
import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        // make a working directory
        args.joinToString(" ").run()

        /*
        val workingDir = File(System.getProperty("user.dir") + "/working")
        workingDir.deleteRecursively()
        workingDir.mkdirs()
        System.setProperty("user.dir", workingDir.path)

        "init badr".run()
        val path = System.getProperty("user.dir")
        "config user.name \"example\" user.email \"example@gmail.com\"".run()
        "touch $path/file.txt".runCommand()
        "add $path/file.txt".run()
        "status".run()
        "commit -m \"init\"".run()
        "log".run()
        "touch $path/file2.txt".runCommand()
        "status".run()
        "add $path/file2.txt".run()
        "status".run()
        "commit -m \"echo\"".run()
        "branch new".run()
        "checkout new".run()
        "log".run()
        "git".run()
        */

    }

    private fun String.isValid(regex: Regex) = regex.matches(this)
    private fun String.run() {
        val args = this.split(" ")
        val regex: Regex
        when (args[0]) {
            "git" -> {
                regex = Regex("""git""")
                if (!this.isValid(regex)) {
                    println("usage: git")
                    return
                }
                // rename .kit to .git
                val kitDir = File("${System.getProperty("user.dir")}/.kit")
                val gitDir = File("${System.getProperty("user.dir")}/.git")
                kitDir.renameTo(gitDir)
            }

            "init" -> {
                regex = Regex("""init( (\S+))?""")
                if (!this.isValid(regex)) {
                    println("usage: init [directory]")
                    return
                }
                val matchResult = regex.find(this)
                val directory = matchResult?.groupValues?.get(2) ?: ""
                init(directory)
            }

            "config" -> {
                // config user.name "Badr" user.email "email"
                regex = Regex("""config user.name ([^"]+) user.email ([^"]+)""")
                if (!this.isValid(regex)) {
                    println("usage: config user.name \"name\" user.email \"email\"")
                    return
                }
                val matchResult = regex.find(this)
                val name = matchResult!!.groupValues[1]
                val email = matchResult.groupValues[2]
                Config.set("user.name", name)
                Config.set("user.email", email)
            }

            "add" -> {
                regex = Regex("""add (\S+)""")
                if (!this.isValid(regex)) {
                    println("usage: add <file>")
                    return
                }
                val matchResult = regex.find(this)
                val path = matchResult?.groupValues?.get(1)!!
                if (path.startsWith(System.getProperty("user.dir")))
                    add(path)
                else
                    add(System.getProperty("user.dir") + "/" + path)
            }

            "unstage" -> {
                regex = Regex("""unstage (\S+)""")
                if (!this.isValid(regex)) {
                    println("usage: unstage <file>")
                    return
                }
                val matchResult = regex.find(this)
                val path = matchResult?.groupValues?.get(1)!!
                unstage(path)
            }

            "status" -> {
                regex = Regex("""status""")
                if (!this.isValid(regex)) {
                    println("usage: status")
                    return
                }
                status()
            }

            "commit" -> {
                regex = Regex("""commit (-m ([^"]+))""")
                if (!this.isValid(regex)) {
                    println("usage: commit -m \"message\"")
                    return
                }
                val matchResult = regex.find(this)
                val message = matchResult?.groupValues?.get(2)!!
                commit(message)
            }

            "log" -> {
                regex = Regex("""log""")
                if (!this.isValid(regex)) {
                    println("usage: log")
                    return
                }
                log()
            }

            "branch" -> {
                regex = Regex("""branch (\S+)?""")
                if (!this.isValid(regex)) {
                    println("usage: branch <branch name> [commit hash]")
                    return
                }
                val matchResult = regex.find(this)
                val branchName = matchResult?.groupValues?.get(1)!!
                branch(branchName)
            }

            "checkout" -> {
                regex = Regex("""checkout (\S+)""")
                if (!this.isValid(regex)) {
                    println("usage: checkout <branch name> | <commit hash>")
                    return
                }
                val matchResult = regex.find(this)
                val dest = matchResult?.groupValues?.get(1)!!
                checkout(dest)
            }

            in arrayOf("help", "-h", "--help", "--h") -> {
                help()
            }

            else -> {
                help()
            }
        }
    }

    private fun help() {
        println("usage: kit <command> [<args>]")
        println("The most commonly used git commands are:")
        println("   init       Create an empty kit repository or reinitialize an existing one")
        println("   config     Set kit user name and email")
        println("   add        Add file contents to the index")
        println("   unstage    Remove file contents from the index")
        println("   status     Show the working tree status")
        println("   commit     Make a commit")
        println("   log        Show commit logs")
        println("   branch     Create branches")
        println("   checkout   Switch branches or restore working tree files")
        println("   help       Display help information")
        println("   git        Convert kit repository to git repository (basically rename .kit to .git)")
    }
}