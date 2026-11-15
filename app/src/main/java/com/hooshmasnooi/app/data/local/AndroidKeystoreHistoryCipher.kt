package com.hooshmasnooi.app.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption for local chat history.
 *
 * The key is generated on-device and remains non-exportable in Android Keystore.
 * The binary envelope is: magic(4) + version(1) + nonceLength(1) + nonce + ciphertext/tag.
 */
internal interface HistoryCipher {
    fun encrypt(plainText: ByteArray): ByteArray
    fun decrypt(envelope: ByteArray): ByteArray
}

internal class AndroidKeystoreHistoryCipher : HistoryCipher {
    override fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(plainText)
        val nonce = cipher.iv
        require(nonce.size == GCM_NONCE_BYTES) { "Unexpected AES-GCM nonce length" }

        return ByteBuffer.allocate(HEADER_SIZE + nonce.size + encrypted.size)
            .put(MAGIC)
            .put(FORMAT_VERSION)
            .put(nonce.size.toByte())
            .put(nonce)
            .put(encrypted)
            .array()
    }

    override fun decrypt(envelope: ByteArray): ByteArray {
        require(envelope.size > HEADER_SIZE + GCM_NONCE_BYTES + GCM_TAG_BYTES) {
            "Encrypted history file is too short"
        }
        val buffer = ByteBuffer.wrap(envelope)
        val magic = ByteArray(MAGIC.size)
        buffer.get(magic)
        require(magic.contentEquals(MAGIC)) { "Unknown encrypted history format" }

        val version = buffer.get()
        require(version == FORMAT_VERSION) { "Unsupported encrypted history version" }
        val nonceLength = buffer.get().toInt() and 0xff
        require(nonceLength == GCM_NONCE_BYTES) { "Invalid encrypted history nonce" }
        require(buffer.remaining() > nonceLength + GCM_TAG_BYTES) {
            "Encrypted history payload is incomplete"
        }

        val nonce = ByteArray(nonceLength)
        buffer.get(nonce)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, nonce))
        cipher.updateAAD(AAD)
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "atlas_local_history_aes_gcm_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        const val GCM_NONCE_BYTES = 12
        const val HEADER_SIZE = 6
        const val FORMAT_VERSION: Byte = 1
        val MAGIC = byteArrayOf(0x41, 0x54, 0x4c, 0x48) // "ATLH"
        val AAD = "atlas-chat-history:v1".toByteArray(Charsets.UTF_8)
    }
}
