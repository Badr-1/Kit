package kit.porcelain

import kit.plumbing.*
import kit.porcelain.ChangeType.*
import kit.utils.*
import java.io.File
import java.time.*
import java.util.*


/**
 * Initialize a new repository or reinitialize an existing one
 * @param repositoryName the name of the repository, if empty the current directory will be used
 * @return a message indicating the result of the operation
 */
fun init(repositoryName: String = ""): String {
    var path = System.getProperty("user.dir")
    if (repositoryName.isNotEmpty()) {
        File("$path/$repositoryName").mkdir()
        if (!path.endsWith("/$repositoryName")) {
            path += "/$repositoryName"
        }
        System.setProperty("user.dir", path)
    }
    path += '/'
    if (File("$path.kit").exists()) {
        return "Reinitialized existing Kit repository in ${File("${path}.kit").absolutePath}".apply { println(this) }
    }
    // repository
    println("Creating ${File("${path}.kit").relativePath()} directory".green())
    File("${path}.kit").mkdir()
    // objects database
    println("Creating ${File("${path}.kit/objects").relativePath()} object database directory".green())
    File("${path}.kit/objects").mkdir()
    // refs
    println("Creating ${File("${path}.kit/refs").relativePath()} directory".green())
    File("${path}.kit/refs").mkdir()
    // heads
    println("Creating ${File("${path}.kit/refs/heads").relativePath()} for branches directory".green())
    File("${path}.kit/refs/heads").mkdir()
    // HEAD
    println("Creating ${File("${path}.kit/HEAD").relativePath()} file".green())
    File("${path}.kit/HEAD").writeText("ref: refs/heads/master")
    println("Writing default branch name to HEAD".green())
    // config
    // default config
    Config.unset()
    Config.write()
    println("Writing default config to ${File("${path}.kit/config").relativePath()}".green())
    return "Initialized empty Kit repository in ${File("${path}.kit").absolutePath}".apply { println(this) }
}

/**
 * Add file to staging area
 * @param filePath the path of the file to be added
 */
fun add(filePath: String) {
    // check if the file exists
    if (!File(filePath).exists()) {
        throw Exception("fatal: pathspec '$filePath' did not match any files")
    }
    // check if the file is in the working directory
    if (!File(filePath).absolutePath.startsWith(File(System.getProperty("user.dir")).absolutePath)) {
        throw Exception("fatal: pathspec '$filePath' is outside repository")
    }
    // check if the file is in the .kit directory
    if (File(filePath).absolutePath.startsWith(File("${System.getProperty("user.dir")}/.kit").absolutePath)) {
        return
    }
    val file = File(filePath)
    // update the index
    val mode = getMode(file)
    updateIndex(filePath, "-a", hashObject(filePath, write = true), mode)
}

/**
 * Remove file from staging area
 * @param filePath the path of the file to be removed
 */
fun unstage(filePath: String) {
    // check if the file is in the working directory
    if (!File(filePath).absolutePath.startsWith(File(System.getProperty("user.dir")).absolutePath)) {
        throw Exception("fatal: pathspec '$filePath' is outside repository")
    }
    // check if the file exists or is in the index
    if (GitIndex.get(File(filePath).relativePath()) == null) {
        throw Exception("fatal: pathspec '$filePath' did not match any files")
    }
    updateIndex(filePath, "-d")
}

/**
 * return the status of the repository
 * @return the status of the repository
 */
fun status(): String {
    // get all the files in the working directory except the .kit directory
    val workingDirectoryFiles =
        File(System.getProperty("user.dir")).walk().filter { it.isFile && !it.path.contains(".kit") }.toList()
            .map { it.relativePath() }

    // get all the files in the index
    val indexFiles = GitIndex.entries().map { it.path }

    // get all files in the HEAD commit tree
    val headCommitTreeFiles = getHeadCommitTreeFiles()

    val untrackedFiles = mutableListOf<Change>()
    val unStagedChanges = mutableListOf<Change>()
    val stagedChanges = mutableListOf<Change>()

    // untracked files are files that are in the working directory but not in the index

    untrackedFiles.addAll(workingDirectoryFiles.filter { !indexFiles.contains(it) }
        .map { Change(UNTRACKED, it) })

    /**
     * unStaged changes are the following (index vs working directory):
     * 1. content: the sha1 is different => modified
     * 2. mode: the mode is different => modified
     * 3. deleted: the file is deleted => deleted
     */
    // add modified files (content)
    unStagedChanges.addAll(
        workingDirectoryFiles.filter { indexFiles.contains(it) }
            .filter { GitIndex.get(it)!!.sha1 != hashObject("${System.getProperty("user.dir")}/$it") }
            .map { Change(MODIFIED, it) }
    )
    // add modified files (mode)
    unStagedChanges.addAll(
        workingDirectoryFiles.filter { indexFiles.contains(it) }
            .filter { GitIndex.get(it)!!.mode != getMode(File("${System.getProperty("user.dir")}/$it")).toInt(8) }
            .map { Change(MODIFIED, it) }
    )
    // add deleted files
    unStagedChanges.addAll(
        indexFiles.filter { !workingDirectoryFiles.contains(it) }
            .map { Change(DELETED, it) }
    )
    /**
     * staged changes are the following (index vs HEAD commit tree):
     * 1. content: the sha1 is different => modified
     * 2. mode: the mode is different => modified
     * 3. deleted: the file is deleted => deleted
     * 4. added: the file is added => added
     */
    if (headCommitTreeFiles.isEmpty()) {
        // add added files
        stagedChanges.addAll(
            workingDirectoryFiles.filter { indexFiles.contains(it) }
                .map { Change(ADDED, it) }
        )
    } else {
        // add added files
        stagedChanges.addAll(
            indexFiles.filter { !headCommitTreeFiles.map { headEntry -> headEntry.path }.contains(it) }
                .map { Change(ADDED, it) }
        )
        // add modified files (content)
        stagedChanges.addAll(
            indexFiles.filter { headCommitTreeFiles.map { headEntry -> headEntry.path }.contains(it) }
                .filter { common -> GitIndex.get(common)!!.sha1 != headCommitTreeFiles.find { it.path == common }!!.hash }
                .map { Change(MODIFIED, it) }
        )
        // add modified files (mode)
        stagedChanges.addAll(
            indexFiles.filter { headCommitTreeFiles.map { headEntry -> headEntry.path }.contains(it) }
                .filter { common ->
                    GitIndex.get(common)!!.mode != headCommitTreeFiles.find { it.path == common }!!.mode.toInt(
                        8
                    )
                }
                .map { Change(MODIFIED, it) }
        )
        // add deleted files
        stagedChanges.addAll(
            headCommitTreeFiles.map { it.path }.filter { !indexFiles.contains(it) }
                .map { Change(DELETED, it) }
        )
    }


    return statusString(untrackedFiles, stagedChanges, unStagedChanges).apply { println(this) }
}

// TODO think about adding amend option
/**
 * commit the current index
 * @param message the commit message
 * @return the commit hash
 */
fun commit(message: String): String {
    val head = getHead()
    val parent = when {
        head.matches(Regex("[0-9a-f]{40}")) -> head
        else -> {
            if (File("${System.getProperty("user.dir")}/.kit/refs/heads/$head").exists()) getBranchCommit(head)
            else ""
        }
    }
    if (parent.isNotEmpty()) {
        val parentTree = getTreeHash(parent)
        val indexTree = writeTree(System.getProperty("user.dir"), write = false)
        if (parentTree == indexTree) {
            throw Exception("nothing to commit, working tree clean")
        }
    }
    if (writeTree(System.getProperty("user.dir"), write = false).isEmpty()) {
        throw Exception("nothing to commit, working tree clean")
    }

    val treeHash = writeTree(System.getProperty("user.dir"), write = true)
    val commitHash = commitTree(treeHash, message, parent)
    if (head.matches(Regex("[0-9a-f]{40}"))) {
        File("${System.getProperty("user.dir")}/.kit/HEAD").writeText(commitHash)
    } else {
        val branch = File("${System.getProperty("user.dir")}/.kit/refs/heads/$head")
        branch.createNewFile()
        branch.writeText(commitHash)
    }
    return commitHash
}

/**
 * checkout HEAD to a specific commit or branch
 * @param ref the commit hash or branch name
 * @throws Exception if the ref is not a commit hash or a branch name
 */
fun checkout(ref: String) {
    println("Checking out ${ref.red()}".blue())
    // ref could be a commit hash or a branch name
    val commitHash = when {
        // TODO think about supporting working with commit hashes less than 40 characters (short commit hashes)
        // TODO think about checking out files
        ref.matches(Regex("[0-9a-f]{40}")) -> {
            // change the HEAD
            File("${System.getProperty("user.dir")}/.kit/HEAD").writeText(ref)
            println("Writing ${ref.red()} to ${".kit/HEAD".blue()}")
            ref
        }

        else -> {
            val branch = File("${System.getProperty("user.dir")}/.kit/refs/heads/$ref")
            val tag = File("${System.getProperty("user.dir")}/.kit/refs/tags/$ref")
            if (!branch.exists() && !tag.exists()) {
                throw Exception("error: pathspec '$ref' did not match any file(s) known to kit")
            }
            if (branch.exists()) {
                // change the HEAD
                File("${System.getProperty("user.dir")}/.kit/HEAD").writeText("ref: refs/heads/$ref")
                println("Writing ${"ref: refs/heads/$ref".red()} to ${".kit/HEAD".blue()}")
                branch.readText()
            } else // tag
            {
                // change the HEAD
                val tagCommit = getContent(tag.readText()).split("\n")[0].split(" ")[1]
                File("${System.getProperty("user.dir")}/.kit/HEAD").writeText(tagCommit)
                println("Writing ${tagCommit.red()} to ${".kit/HEAD".blue()}")
                tagCommit
            }

        }
    }

    updateWorkingDirectory(commitHash)
}

/**
 * log the history of the repository
 * it traverses from the HEAD commit to the first commit
 */
fun log() {
    val commits = getHistory()
    if (commits.isEmpty())
        return
    val branches = getBranches()
    val tags = getTags()
    val refs = branches + tags
    val head = when {
        File("${System.getProperty("user.dir")}/.kit/HEAD").readText().matches(Regex("[0-9a-f]{40}")) -> getHead()
        else -> {
            var head = File("${System.getProperty("user.dir")}/.kit/HEAD").readText().split(" ")[1]
            head =
                File("${System.getProperty("user.dir")}/.kit/$head").relativeTo(File("${System.getProperty("user.dir")}/.kit/refs/heads")).path
            head
        }
    }
    for (commit in commits) {
        val commitContent = getContent(commit)
        val hasParent = if (commitContent.contains("parent")) 0 else 1
        val authorLine = commitContent.split("\n")[2 - hasParent].split(" ").toMutableList()
        authorLine.removeFirst()
        authorLine.removeLast()
        val unixEpoch = authorLine.removeLast()
        val date = Date(unixEpoch.toLong() * 1000)
        val today = Date()
        val timeDifference = calculateDateTimeDifference(
            date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        ).green()
        val author = "<${authorLine.joinToString(" ")}>".blue()
        val message = commitContent.split("\n")[4 - hasParent]
        val commitRefs = refs.filter { it.value == commit }.map { it.key }.toMutableList()
        if (commit == head) {
            commitRefs.add("HEAD")
        } else if (commitRefs.contains(head)) {
            commitRefs.remove(head)
            commitRefs.add("HEAD -> $head")
        }
        val commitHash = commit.substring(0, 7).red()
        val commitRefsString = if (commitRefs.isNotEmpty()) " (${commitRefs.joinToString()})".yellow() else ""
        println(
            "* $commitHash$commitRefsString $message $timeDifference $author"
        )
    }
}


/**
 * create a new branch
 * @param branchName the name of the branch
 * @param ref the commit hash to point to
 */
fun branch(branchName: String, ref: String = "HEAD") {
    val branch = File("${System.getProperty("user.dir")}/.kit/refs/heads/$branchName")
    if (branch.exists()) {
        throw Exception("fatal: A branch named '$branchName' already exists.")
    } else {
        if (branchName.contains("/")) {
            branch.parentFile.mkdirs()
        }
        branch.createNewFile()
    }
    when (ref) {
        "HEAD" -> {
            val head = getHead()
            val dest = when {
                head.matches(Regex("[0-9a-f]{40}")) -> head
                else -> {
                    getBranchCommit(head)
                }
            }
            println("Created branch " + branchName.green() + " at " + dest.substring(0, 7).red())
            println("file: " + branch.relativePath().green())
            branch.writeText(dest)
        }

        else -> {
            println("Created branch " + branchName.green() + " at " + ref.substring(0, 7).red())
            println("file: " + branch.relativePath().green())
            branch.writeText(ref)
        }
    }

}


/**
 * creates an annotated tag
 * @param tagName the name of the tag
 * @param tagMessage the message of the tag
 * @param objectHash the hash of the object to tag
 * @param tagType the type of the object to tag
 * @throws Exception if the tag already exists
 * @return the hash of the tag
 */
fun tag(tagName: String, tagMessage: String, objectHash: String = getHEADHash(), tagType: String = "commit"): String {
    val tagFile = File("${System.getProperty("user.dir")}/.kit/refs/tags/$tagName")
    // if tag has already been created throw an exception
    if (tagFile.exists()) {
        throw Exception("fatal: tag '$tagName' already exists")
    }
    val content =
        "object $objectHash\ntype $tagType\ntag $tagName\ntagger ${Config.get("user.name")} <${Config.get("user.email")}> ${System.currentTimeMillis() / 1000} +0200\n\n$tagMessage\n"
    val hash = hashObject(content, "tag", true)

    // if refs/tags doesn't exist create it
    val tagsDirectory = File("${System.getProperty("user.dir")}/.kit/refs/tags")
    if (!tagsDirectory.exists()) {
        tagsDirectory.mkdirs()
    }

    // if tag has / create parent directories
    if (tagName.contains("/")) {
        if (!tagFile.parentFile.exists()) {
            tagFile.parentFile.mkdirs()
        }
    }
    println("Created tag " + tagName.green() + " for " + objectHash.substring(0, 7).red())
    println("file: " + tagFile.relativePath().green())
    tagFile.writeText(hash)
    return hash
}


/********** helper functions **********/

/**
 * get the commit hash of a branch
 * @param branch the name of the branch
 * @return the commit hash of the branch
 * @throws Exception if the branch does not exist
 */
private fun getBranchCommit(branch: String): String {
    if (!File("${System.getProperty("user.dir")}/.kit/refs/heads/$branch").exists()) {
        throw Exception("fatal: Not a valid object name: '$branch'.")
    }
    return File("${System.getProperty("user.dir")}/.kit/refs/heads/$branch").readText()
}

/**
 * update the working directory to the state of a commit
 * @param commitHash the hash of the commit
 */
fun updateWorkingDirectory(commitHash: String) {

    println("Removing Files From Working Directory and Index".red())
    // delete all files that are in the index
    val index = GitIndex.entries()
    index.forEach {
        val file = File("${System.getProperty("user.dir")}/${it.path}")
        updateIndex(file.path, "-d")
        if (file.exists()) {
            file.delete()
            // delete empty directories
            var parent = file.parentFile
            while (parent.listFiles()!!.isEmpty()) {
                parent.delete()
                parent = parent.parentFile
            }
        }
    }
    println("Updating Files in Working Directory and Index".blue())
    // get the tree hash from the commit
    val treeHash = getTreeHash(commitHash)
    val treeEntries = getTreeEntries(treeHash)
    treeEntries.forEach {
        val file = File("${System.getProperty("user.dir")}/${it.path}")
        // create directories if necessary
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        file.createNewFile()
        file.writeText(getContent(it.hash))
        if (it.mode == "100755") {
            file.setExecutable(true)
        }
        updateIndex(file.path, "-a", hashObject(file.path), it.mode)
    }
}

/**
 * get the tree hash from a commit
 * @param commitHash the hash of the commit
 * @return the hash of the tree
 */
fun getTreeHash(commitHash: String): String {
    val content = getContent(commitHash)
    return content.split("\n")[0].split(" ")[1]
}

/**
 * get the tree entries of a tree
 * @param treeHash the hash of the tree
 * @return a list of tree entries
 */
fun getTreeEntries(treeHash: String): List<TreeEntry> {
    val content = getContent(treeHash)
    val treeEntries = mutableListOf<TreeEntry>()
    content.split("\n").map {
        val mode = it.split(" ")[0]
        val sha1 = it.split(" ")[2].split("\t")[0]
        val path = it.split("\t")[1]
        if (mode == "40000") {
            treeEntries.addAll(getTreeEntries(sha1).map { treeEntry ->
                TreeEntry(
                    treeEntry.mode, "$path/${treeEntry.path}", treeEntry.hash
                )
            })
        } else {
            treeEntries.add(TreeEntry(mode, path, sha1))
        }
    }
    return treeEntries
}


enum class ChangeType {
    ADDED, MODIFIED, DELETED, UNTRACKED;

    override fun toString() = when (this) {
        ADDED -> "A"
        MODIFIED -> "M"
        DELETED -> "D"
        UNTRACKED -> "??"
    }
}

data class Change(val type: ChangeType, val path: String)

/**
 * helper function that returns the status of the repository as a string
 * @param untrackedFiles the list of untracked files
 * @param staged the list of staged changes
 * @param unStaged the list of unStaged changes
 * @return the status of the repository as a string
 */
fun statusString(
    untrackedFiles: List<Change>,
    staged: List<Change>,
    unStaged: List<Change>,
): String {
    val head = getHead()
    val onWhat = if (head.matches(Regex("[0-9a-f]{40}"))) {
        "Head detached at ${head.substring(0, 7).red()}"
    } else {
        "On branch ${head.green()}"
    }
    val status = mutableListOf<String>()
    status += onWhat
    status += untrackedFiles.sortedBy { it.path }.joinToString("\n\t") { "${it.type} ${it.path}".red() }
        .ifEmpty { "" }
    status += staged.sortedBy { it.path }.joinToString("\n\t") { "${it.type} ${it.path}".green() }
        .ifEmpty { "" }
    status += unStaged.sortedBy { it.path }.joinToString("\n\t") { "${it.type} ${it.path}".yellow() }
        .ifEmpty { "" }
    status.removeIf { it.isEmpty() }
    return status.joinToString("\n\t")
}

/**
 * helper function that returns the list of files in the HEAD commit tree
 * @return the list of files in the HEAD commit tree
 */
fun getHeadCommitTreeFiles(): MutableList<TreeEntry> {
    val head = getHead()
    return if (!head.matches(Regex("[0-9a-f]{40}"))) {
        return try {
            val branch = getBranchCommit(head)
            getTreeEntries(getTreeHash(branch)).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    } else {
        getTreeEntries(getTreeHash(head)).toMutableList()
    }

}


/**
 * get the commit hash of a branch that is pointed to by HEAD
 */
fun getHead(): String {
    val head = File("${System.getProperty("user.dir")}/.kit/HEAD").readText()
    return if (head.contains("ref: refs/heads/")) {
        val branch =
            File(System.getProperty("user.dir") + "/.kit/" + head.split(" ")[1]).relativePath("${System.getProperty("user.dir")}/.kit/refs/heads")
        branch
    } else {
        head
    }
}

/**
 * this is a helper function for the actual log command
 * @return the list of commits
 */
fun getHistory(): List<String> {
    val commits = mutableListOf<String>()
    val head = getHead()
    var it = head
    if (!head.matches(Regex("[0-9a-f]{40}"))) { // if the head is a branch
        try {
            it = getBranchCommit(head) // throw an exception if the branch doesn't exist
        } catch (e: Exception) {
            return commits // return an empty list
        }
    }
    do {
        commits.add(it)
        it = getParent(it)
    } while (it != "")

    return commits
}

/**
 * calculate the difference between two dates
 * @param startDate the start date
 * @param endDate the end date
 * @return the difference between the two dates in the most significant time unit
 */
fun calculateDateTimeDifference(startDate: LocalDateTime, endDate: LocalDateTime): String {
    val duration = Duration.between(startDate, endDate)
    val period = Period.between(startDate.toLocalDate(), endDate.toLocalDate())

    val years = period.years
    val months = period.months
    val days = period.days

    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    // return the most significant time unit
    return "(${
        when {
            years > 0 -> "$years year${if (years > 1) "s" else ""} ago"
            months > 0 -> "$months month${if (months > 1) "s" else ""} ago"
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            seconds > 0 -> "$seconds second${if (seconds > 1) "s" else ""} ago"
            else -> "just now"
        }
    })"
}

/**
 * get the parent of a commit
 * @param commitHash the hash of the commit
 * @return the hash of the parent commit
 */
fun getParent(commitHash: String): String {
    val content = getContent(commitHash)
    return if (content.contains("parent")) {
        content.split("\n")[1].split(" ")[1]
    } else {
        ""
    }
}


/**
 * this is a helper function for the actual log command
 * @return the list of branches
 */
fun getBranches(): MutableMap<String, String> {
    val refs = mutableMapOf<String, String>()
    val branches = File("${System.getProperty("user.dir")}/.kit/refs/heads").walk().filter { it.isFile }.toList()
    for (branch in branches) {
        refs[branch.relativePath("${System.getProperty("user.dir")}/.kit/refs/heads")] = branch.readText()
    }
    return refs
}

fun getTags(): Map<String, String> {
    val tags = mutableMapOf<String, String>()
    val tagDir = File("${System.getProperty("user.dir")}/.kit/refs/tags")
    if (tagDir.exists()) {
        tagDir.walk().forEach {
            if (it.isFile) {
                val tagHash = it.readText()
                val tagName = it.relativePath("${System.getProperty("user.dir")}/.kit/refs/tags")
                val tagContent = getContent(tagHash)
                val tagCommit = tagContent.split("\n")[0].split(" ")[1]
                tags["tag: $tagName"] = tagCommit
            }
        }
    }
    return tags
}


/**
 * get the commit hash of the HEAD
 * @return the commit hash of the HEAD
 */
fun getHEADHash(): String {
    val workingDirectory = System.getProperty("user.dir")
    val headFile = File("${workingDirectory}/.kit/HEAD")
    val head = headFile.readText()
    return if (head.startsWith("ref:")) {
        val ref = head.substringAfter("ref:").trim()
        val refFile = File("${workingDirectory}/.kit/$ref")
        if (!refFile.exists()) {
            throw Exception("fatal: Failed to resolve 'HEAD' as a valid ref.")
        }
        refFile.readText()
    } else {
        head
    }
}
/****************************************/