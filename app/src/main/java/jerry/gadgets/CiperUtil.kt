package jerry.gadgets

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream


import java.math.BigInteger
import java.security.MessageDigest

private val logger = Logger("CIPER_UTIL")

object CiperUtil {

    var applicationContext:Context? = null

    private var _key:MasterKey? = null

    private fun getKey():MasterKey {
        if (_key == null) {
            _key = MasterKey.Builder(applicationContext!!)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        }

        return _key!!
    }


    fun decryptFileToByteArray(input:File, output:ByteArrayOutputStream): Boolean {


        val encryptedFile = EncryptedFile.Builder(
            applicationContext!!,
            input,
            getKey(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        var inputStream:InputStream? = null
        try {
            inputStream = encryptedFile.openFileInput()
            val buf = ByteArray(4096)
            var read: Int = 0
            while (read >= 0) {
                output.write(buf, 0, read)
                read = inputStream.read(buf)
            }

//            logger.debug("decryption succeeded with: $input")
            return true
        } catch (e:Throwable) {
            logger.warn{"decipher failed, $e"}
            return false
        } finally {
            try {inputStream?.close()} catch(e:Throwable) {}
        }
    }

    /**
     * Caution: file can not be renamed after encryption.
     *
     */
    fun encryptByteArrayToFile(input:ByteArray, output:File):Boolean {
        // Create a file with this name, or replace an entire existing file
        // that has the same name. Note that you cannot append to an existing file,
        // and the file name cannot contain path separators.
        val encryptedFile = EncryptedFile.Builder(
            applicationContext!!,
            output,
            getKey(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return try {
            encryptedFile.openFileOutput().apply {
                write(input)
                flush()
                close()
            }
//            logger.debug("encryption succeeded with: $output")
            true
        } catch (e:Exception) {
            logger.warn{"encryption failed, $e"}
            false
        }
    }
}

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0').lowercase()
}
