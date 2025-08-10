package com.kashif.passwordmanager.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CredentialEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CredentialDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao

    companion object {
        @Volatile
        private var INSTANCE: CredentialDatabase? = null

        fun getDatabase(context: Context): CredentialDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CredentialDatabase::class.java,
                    "credential_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}