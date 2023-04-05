package kit.porcelain

import java.io.File

/**
 * a singleton object that represents the config file
 */
object Config {
    /**
     * consists of the following:
     * - sections, eg [ core ]
     * - keys, eg repositoryformatversion
     * - values, eg 0
     * */
    private fun getConfigFile() = File("${System.getProperty("user.dir")}/.kit/config")
    private val sections = mutableListOf("core") // to be updated when more sections are added
    private val values = mutableMapOf(
        "core" to mutableMapOf(
            "repositoryformatversion" to "0", "filemode" to "true", "bare" to "false", "logallrefupdates" to "true"
        )
    )

    /**
     * unset the config file to its default values
     */
    fun unset() {
        sections.removeIf { it != "core" }
    }

    /**
     * write the config file
     */
    fun write() {
        getConfigFile().createNewFile()
        getConfigFile().writeText("")
        for (section in sections) {
            getConfigFile().appendText("[$section]\n")
            for ((key, value) in values[section]!!) {
                getConfigFile().appendText("\t$key = $value\n")
            }
        }
    }

    /**
     * set a value in the config file and write it
     * @param sectionWithKey the section and key separated by a dot, e.g. core.repositoryformatversion
     * @param value the value to be set
     */
    fun set(sectionWithKey: String, value: String) {
        val section = sectionWithKey.split(".")[0]
        val key = sectionWithKey.split(".")[1]
        if (!sections.contains(section)) {
            sections.add(section)
            values[section] = mutableMapOf()
        }
        values[section]!![key] = value
        write()
    }

    /**
     * get a value from the config file
     * @param sectionWithKey the section and key separated by a dot, e.g. core.repositoryformatversion
     * @return the value
     */
    fun get(sectionWithKey: String): String {
        val section = sectionWithKey.split(".")[0]
        val key = sectionWithKey.split(".")[1]
        if (section == "user" && (key == "name" || key == "email")) {
            if (!sections.contains(section)) {
                sections.add(section)
                values[section] = mutableMapOf()
            }
            if (values[section]!![key] == null) values[section]!![key] = "Kit $key"
        }
        return values[section]!![key]!!
    }
}