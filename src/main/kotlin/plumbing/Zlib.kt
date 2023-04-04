package plumbing

import java.io.ByteArrayOutputStream
import java.util.zip.*

object Zlib {

    @JvmStatic
    fun deflate(content: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(content)
        deflater.finish()
        val buffer = ByteArray(1024)
        val outputStream = ByteArrayOutputStream()
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        outputStream.close()
        return outputStream.toByteArray()
    }

    @JvmStatic
    fun inflate(content: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(content)
        val buffer = ByteArray(1024)
        val outputStream = ByteArrayOutputStream()
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        outputStream.close()
        return outputStream.toByteArray()
    }
}
