package com.nora.tunnel.data.repository

import com.nora.tunnel.data.database.ProfileDao
import com.nora.tunnel.data.database.TunnelProfile
import com.nora.tunnel.data.secure.SecureStorage
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val dao: ProfileDao, private val secure: SecureStorage) {
    fun observe(): Flow<List<TunnelProfile>> = dao.observeAll()

    suspend fun get(id: String): TunnelProfile? = dao.get(id)

    // Secrets go to SecureStorage; only the reference is stored in Room
    suspend fun save(profile: TunnelProfile, secret: String? = null) {
        val ref = if(secret != null) {
            val r = profile.credentialRef ?: "cred_${profile.id}"
            secure.put(r, secret)
            r
        } else profile.credentialRef
        dao.upsert(profile.copy(credentialRef = ref, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(profile: TunnelProfile) = dao.delete(profile)
}
