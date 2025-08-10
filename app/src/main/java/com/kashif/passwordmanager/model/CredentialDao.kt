package com.kashif.passwordmanager.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

// Database
@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials ORDER BY lastModified DESC")
    suspend fun getAllCredentials(): List<CredentialEntity>

    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getCredentialById(id: String): CredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: CredentialEntity)

    @Update
    suspend fun updateCredential(credential: CredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)

    @Query("UPDATE credentials SET accessCount = accessCount + 1 WHERE id = :id")
    suspend fun incrementAccessCount(id: String)
}