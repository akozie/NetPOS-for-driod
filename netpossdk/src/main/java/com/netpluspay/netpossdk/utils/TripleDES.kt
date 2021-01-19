package com.netpluspay.netpossdk.utils

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.experimental.xor
/**
 * @author Derek
 */
internal object TripleDES {
    /**
     * get correct length key for triple DES operation
     * @param key
     * @return
     */
    @JvmStatic
    private fun GetKey(key: ByteArray): ByteArray {
        val bKey = ByteArray(24)
        var i: Int
        if (key.size == 8) {
            i = 0
            while (i < 8) {
                bKey[i] = key[i]
                bKey[i + 8] = key[i]
                bKey[i + 16] = key[i]
                i++
            }
        } else if (key.size == 16) {
            i = 0
            while (i < 8) {
                bKey[i] = key[i]
                bKey[i + 8] = key[i + 8]
                bKey[i + 16] = key[i]
                i++
            }
        } else if (key.size == 24) {
            i = 0
            while (i < 24) {
                bKey[i] = key[i]
                i++
            }
        }
        return bKey
    }

    /**
     * encrypt data in ECB mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun encrypt(data: ByteArray?, key: ByteArray): ByteArray? {
//		Log.d(TripleDES.class.getSimpleName(), "Data: " +  Hex2String(data));
//		Log.d(TripleDES.class.getSimpleName(), "Key: " +  Hex2String(key));
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)
            return cipher.doFinal(data)
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * decrypt data in ECB mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun decrypt(data: ByteArray?, key: ByteArray): ByteArray? {
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sk)
            return cipher.doFinal(data)
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * encrypt data in CBC mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun encrypt_CBC(data: ByteArray, key: ByteArray): ByteArray? {
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)
            val enc = ByteArray(data.size)
            val dataTemp1 = ByteArray(8)
            var dataTemp2 = ByteArray(8)
            var i = 0
            while (i < data.size) {
                for (j in 0..7) dataTemp1[j] = (data[i + j] xor dataTemp2[j]) as Byte
                dataTemp2 = cipher.doFinal(dataTemp1)
                for (j in 0..7) enc[i + j] = dataTemp2[j]
                i += 8
            }
            return enc
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }
    @JvmStatic
    fun encrypt_CBC(data: ByteArray, key: ByteArray, IV: ByteArray): ByteArray? {
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)
            val enc = ByteArray(data.size)
            val dataTemp1 = ByteArray(8)
            var dataTemp2 = ByteArray(8)
            for (i in 0..7) dataTemp2[i] = IV[i]
            var i = 0
            while (i < data.size) {
                for (j in 0..7) dataTemp1[j] = (data[i + j] xor dataTemp2[j]) as Byte
                dataTemp2 = cipher.doFinal(dataTemp1)
                for (j in 0..7) enc[i + j] = dataTemp2[j]
                i += 8
            }
            return enc
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    /**
     * decrypt data in CBC mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun decrypt_CBC(data: ByteArray, key: ByteArray): ByteArray? {
        val sk: SecretKey = SecretKeySpec(GetKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sk)
            val enc = cipher.doFinal(data)
            for (i in 8 until enc.size) enc[i] = enc[i] xor data[i - 8]
            return enc
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        } catch (e: NullPointerException) {
        }
        return null
    }

    /**
     * encrypt data in ECB mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun encrypt(data: String, key: String): String {
        val bData: ByteArray
        val bKey: ByteArray
        val bOutput: ByteArray?
        val result: String
        bData = String2Hex(data)
        bKey = String2Hex(key)
        bOutput = encrypt(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    /**
     * decrypt data in ECB mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun decrypt(data: String, key: String): String {
        val bData: ByteArray
        val bKey: ByteArray
        val bOutput: ByteArray?
        val result: String
        bData = String2Hex(data)
        bKey = String2Hex(key)
        bOutput = decrypt(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    /**
     * encrypt data in CBC mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun encrypt_CBC(data: String, key: String): String {
        val bData: ByteArray
        val bKey: ByteArray
        val bOutput: ByteArray?
        val result: String
        bData = String2Hex(data)
        bKey = String2Hex(key)
        bOutput = encrypt_CBC(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    /**
     * decrypt data in CBC mode
     * @param data
     * @param key
     * @return
     */
    @JvmStatic
    fun decrypt_CBC(data: String, key: String): String {
        val bData: ByteArray
        val bKey: ByteArray
        val bOutput: ByteArray?
        val result: String
        bData = String2Hex(data)
        bKey = String2Hex(key)
        bOutput = decrypt_CBC(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    /**
     * Convert Byte Array to Hex String
     * @param data
     * @return
     */
    @JvmStatic
    fun Hex2String(data: ByteArray?): String {
        if (data == null) {
            return ""
        }
        var result = ""
        for (i in data.indices) {
            var tmp: Int = data[i].toInt() shr 4
            result += Integer.toString(tmp and 0x0F, 16)
            tmp = (data[i] and 0x0F).toInt()
            result += Integer.toString(tmp and 0x0F, 16)
        }
        return result
    }

    /**
     * Convert Hex String to byte array
     * @param data
     * @return
     */
    @JvmStatic
    fun String2Hex(data: String): ByteArray {
        val result: ByteArray
        result = ByteArray(data.length / 2)
        var i = 0
        while (i < data.length) {
            result[i / 2] = data.substring(i, i + 2).toInt(16).toByte()
            i += 2
        }
        return result
    }
}