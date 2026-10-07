package com.nora.tunnel.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM TunnelProfile ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TunnelProfile>>

    @Query("SELECT * FROM TunnelProfile WHERE id = :id")
    suspend fun get(id: String): TunnelProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: TunnelProfile)

    @Delete
    suspend fun delete(profile: TunnelProfile)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM ConnectionSession ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ConnectionSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ConnectionSession)
}

@Database(entities = [TunnelProfile::class, ConnectionSession::class], version = 1, exportSchema = false)
abstract class NoraDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao
}
