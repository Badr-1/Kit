package kit.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kit.porcelain.tag

class TagCommand : CliktCommand(name = "tag", help = "create tag") {
    private val tagName by argument(
        help = "The name of the tag",
        name = "tagName"
    )
    private val ref by argument().optional()
    private val message by option("-m", "--message", help = "Tag message").required()
    override fun run() {
        if (ref == null)
            tag(tagName, message)
        else
            tag(tagName, message, ref!!)
    }
}