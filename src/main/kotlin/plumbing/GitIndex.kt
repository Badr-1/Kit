package plumbing

import hexStringToByteArray
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.time.Instant
import kotlin.experimental.and

object GitIndex {
    private val entries = mutableListOf<GitIndexEntry>()
    private lateinit var signature: String
    private var version: Int = 2
    private var entryCount: Int = 0
    private val indexFile = File("${System.getProperty("user.dir")}/.kit/index")

    // get entries count
    fun getEntryCount(): Int {
        return entryCount
    }

    data class GitIndexEntry(
        var ctimeSeconds: Int,
        var ctimeNanoSeconds: Int,
        var mtimeSeconds: Int,
        var mtimeNanoSeconds: Int,
        val dev: Int,
        val ino: Int,
        val mode: Int,
        val uid: Int,
        val gid: Int,
        var fileSize: Int,
        var sha1: String,
        val flags: Int,
        val path: String,
        val padding: Int
    ) {
        fun write() {
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(ctimeSeconds).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(ctimeNanoSeconds).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(mtimeSeconds).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(mtimeNanoSeconds).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(dev).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(ino).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(mode).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(uid).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(gid).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(fileSize).array())
            indexFile.appendBytes(sha1.hexStringToByteArray())
            indexFile.appendBytes(ByteBuffer.allocate(2).putShort(flags.toShort()).array())
            indexFile.appendBytes(path.toByteArray())
            indexFile.appendBytes(ByteArray(padding))
        }
    }

    init {
        refreshIndex()
    }
    fun clearIndex() {
        entries.clear()
        entryCount = 0
    }
    fun refreshIndex() {
        if (indexFile.exists()) {
            val indexBytes: ByteArray = indexFile.readBytes()
            var offset = 0
            // The first 12 bytes of the index file are a header
            signature = String(indexBytes.copyOfRange(0, 4))
            version = (ByteBuffer.wrap(indexBytes.copyOfRange(4, 8)).int)
            entryCount = (ByteBuffer.wrap(indexBytes.copyOfRange(8, 12)).int)
            offset += 12

            println("[header]")
            println("\tsignature: $signature")
            println("\tversion: $version")
            println("\tentryCount: $entryCount")

            // The next section of the index file consists of entry metadata
            for (i in 0 until entryCount) {
                val ctimeSeconds = (ByteBuffer.wrap(indexBytes.copyOfRange(offset, offset + 4)).int)
                val ctimeNanoSeconds = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 4, offset + 8)).int)
                val mtimeSeconds = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 8, offset + 12)).int)
                val mtimeNanoSeconds = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 12, offset + 16)).int)
                val dev = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 16, offset + 20)).int)
                val ino = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 20, offset + 24)).int)
                val mode = ByteBuffer.wrap(indexBytes.copyOfRange(offset + 24, offset + 28)).int
                val uid = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 28, offset + 32)).int)
                val gid = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 32, offset + 36)).int)
                val fileSize = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 36, offset + 40)).getInt())
                // read next 20 bytes for sha1 and decode it to hex string and put it in a variable
                val sha1 = indexBytes.copyOfRange(offset + 40, offset + 60).joinToString("") { "%02x".format(it) }
                val flags = (ByteBuffer.wrap(indexBytes.copyOfRange(offset + 60, offset + 62)).short)
                // 1 bit for assume valid
//                val assumeValid = flags and 0x8000.toShort() != 0.toShort()
                // 1 bit for extended
//                val extended = flags and 0x4000.toShort() != 0.toShort()
                // 1 bit for stageOne
//                val stageOne = flags and 0x2000.toShort() != 0.toShort()
                // 1 bit for stageTwo
//                val stageTwo = flags and 0x1000.toShort() != 0.toShort()
//                val stage = stageOne.toString() + stageTwo.toString()
                // 12 bits for name length
                val nameLength = flags and 0x0FFF.toShort()
                val path = String(indexBytes.copyOfRange(offset + 62, offset + 62 + nameLength))
                offset += 62 + nameLength

                // convert this ((8 - ((62 + nameLength) % 8)) or 8) to int
                val padding = ((8 - ((62 + nameLength) % 8)).coerceAtMost(8))
                // treat padding as a binary number and convert it to int

                // read next padding bytes
//            val paddingBytes = indexBytes.copyOfRange(offset, offset + padding)
                offset += padding

                entries.add(
                    GitIndexEntry(
                        ctimeSeconds,
                        ctimeNanoSeconds,
                        mtimeSeconds,
                        mtimeNanoSeconds,
                        dev,
                        ino,
                        mode,
                        uid,
                        gid,
                        fileSize,
                        sha1,
                        flags.toInt(),
                        path,
                        padding
                    )
                )
            }
            entries.forEachIndexed { index, entry ->
                println("[entry ${index + 1}]")
                println("\tctime: ${entry.ctimeSeconds + entry.ctimeNanoSeconds / 1000000000.0}")
                println("\tmtime: ${entry.mtimeSeconds + entry.mtimeNanoSeconds / 1000000000.0}")
                println("\tdev: ${entry.dev}")
                println("\tino: ${entry.ino}")
                println("\tmode: ${entry.mode}")
                println("\tuid: ${entry.uid}")
                println("\tgid: ${entry.gid}")
                println("\tfileSize: ${entry.fileSize}")
                println("\tsha1: ${entry.sha1}")
                println("\tflags: ${entry.flags}")
                println("\tpath: ${entry.path}")
            }

            // The final section of the index file consists of the SHA1 of the index file
            val sha1 = indexBytes.copyOfRange(offset, offset + 20).joinToString("") { "%02x".format(it) }
            println("[sha1]")
            println("\tsha1: $sha1")
        } else {
            indexFile.createNewFile()
            signature = "DIRC"
            version = 2
            entryCount = 0
            indexFile.writeBytes(signature.toByteArray())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(version).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(entryCount).array())
            val sha1 = sha1(indexFile.readBytes())
            indexFile.appendBytes(sha1.hexStringToByteArray())
            entries.clear()
        }
    }

    fun list(): String {
        return entries.joinToString("\n") { it.path }
    }

    fun add(file: File, sha1: String, cacheInfo: String) {
        // check if the file is already in the index
        if (entries.any { it.path == file.relativeTo(File(System.getProperty("user.dir"))).path }) {
            // check if the file is modified
            val entry = entries.first { it.path == file.relativeTo(File(System.getProperty("user.dir"))).path }
            if (entry.sha1 == sha1) {
                return
            }
            remove(file)
        }
        // write header
        indexFile.writeBytes("".toByteArray())
        indexFile.writeBytes(signature.toByteArray())
        indexFile.appendBytes(ByteBuffer.allocate(4).putInt(version).array())
        indexFile.appendBytes(ByteBuffer.allocate(4).putInt(entryCount + 1).array())

        // add file to entries
        entries.add(makeEntry(file, sha1, cacheInfo))

        // write entries
        entries.forEach { entry ->
            entry.write()
        }

        // write sha1
        indexFile.appendBytes(sha1(indexFile.readBytes()).hexStringToByteArray())
        entryCount++
    }

    fun remove(file: File) {
        // check if the file is in the index
        if (entries.any { it.path == file.relativeTo(File(System.getProperty("user.dir"))).path }) {
            // remove the file from the index
            entries.removeIf { it.path == file.relativeTo(File(System.getProperty("user.dir"))).path }
            // write header
            indexFile.writeBytes("".toByteArray())
            indexFile.writeBytes(signature.toByteArray())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(version).array())
            indexFile.appendBytes(ByteBuffer.allocate(4).putInt(entryCount - 1).array())

            // write entries
            entries.forEach { entry ->
                entry.write()
            }

            // write sha1
            indexFile.appendBytes(sha1(indexFile.readBytes()).hexStringToByteArray())
            entryCount--
        }
    }

    fun get(path: String): GitIndexEntry? {
        return entries.firstOrNull { it.path == path }
    }

    fun entries(): List<GitIndexEntry> {
        return entries.subList(0, entries.size)
    }

    private fun makeEntry(file: File, sha1: String, cacheInfo: String): GitIndexEntry {
        val attr = Files.readAttributes(file.toPath(), "unix:*")
        val creationTime = Instant.parse("${attr["creationTime"]!!}").epochSecond
        val creationNanoTime = Instant.parse("${attr["creationTime"]!!}").nano
        val lastModifiedTime = Instant.parse("${attr["lastModifiedTime"]!!}").epochSecond
        val lastModifiedNanoTime = Instant.parse("${attr["lastModifiedTime"]!!}").nano
        val dev = attr["dev"]!!.toString().toInt()
        val ino = attr["ino"]!!.toString().toInt()
        val mode = cacheInfo.toInt(8)
        val uid = attr["uid"]!!.toString().toInt()
        val gid = attr["gid"]!!.toString().toInt()
        val fileSize = file.readBytes().size
        val name = file.relativeTo(File(System.getProperty("user.dir"))).path
        val flags = 0x0000 + name.length
        val entrySize = 62 + name.length
        val padding = ((8 - ((entrySize) % 8)).coerceAtMost(8))
        return GitIndexEntry(
            creationTime.toInt(),
            creationNanoTime,
            lastModifiedTime.toInt(),
            lastModifiedNanoTime,
            dev,
            ino,
            mode,
            uid,
            gid,
            fileSize,
            sha1,
            flags,
            name,
            padding
        )
    }
}