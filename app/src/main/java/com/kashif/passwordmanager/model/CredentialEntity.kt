package com.kashif.passwordmanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey val id: String,
    val title: String,
    val username: String,
    val encryptedPassword: ByteArray,
    val iv: ByteArray,
    val keyAlias: String,
    val category: String,
    val createdAt: Long,
    val lastModified: Long,
    val accessCount: Int = 0
)