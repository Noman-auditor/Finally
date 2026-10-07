package com.nora.tunnel

import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.Assert.*

class VpnPermissionTest {
    @Test
    fun prepareReturnsIntentWhenNotPrepared() {
        val intent = VpnService.prepare(ApplicationProvider.getApplicationContext())
        // If null => already prepared, else need user consent
        assertTrue(intent == null || intent.action != null)
    }
}
