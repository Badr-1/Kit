package porcelain

import java.io.File


fun init(repositoryName: String = ""): String {
    var path = "${System.getProperty("user.dir")}/"
    if (repositoryName.isNotEmpty()) {
        File("$path/$repositoryName").mkdir()
        path += "$repositoryName/"

        if (File("$path.kit").exists()) {
            return "Reinitialized existing Kit repository in ${File("${path}.kit").absolutePath}"
        }
    }
    // repository
    File("${path}.kit").mkdir()
    // objects database
    File("${path}.kit/objects").mkdir()
    // refs
    File("${path}.kit/refs").mkdir()
    File("${path}.kit/refs/heads").mkdir()
    // HEAD
    File("${path}.kit/HEAD").writeText("ref: refs/heads/master")
    // config
    // default config
    File("${path}.kit/config").writeText("[core]\n\trepositoryformatversion = 0\n\tfilemode = true\n\tbare = false\n\tlogallrefupdates = true\n")

    return "Initialized empty Kit repository in ${File("${path}.kit").absolutePath}"
}
