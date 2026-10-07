package com.nora.tunnel

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.nora.tunnel.tunnel.TunnelManager
import com.nora.tunnel.tunnel.TunnelState
import com.nora.tunnel.ui.navigation.NoraNavGraph
import com.nora.tunnel.ui.theme.NoraTheme

class MainActivity : ComponentActivity() {
    private var pendingProfile: com.nora.tunnel.data.database.TunnelProfile? = null

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            pendingProfile?.let { profile ->
                startForegroundService(Intent(this, com.nora.tunnel.tunnel.TunnelService::class.java).apply {
                    action = "CONNECT"
                    putExtra("profileId", profile.id)
                })
            }
        } else {
            TunnelManager.notifyState(TunnelState.Error("Permission denied","VPN permission was denied. Nora Tunnel cannot create VPN interface without user consent."))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoraTheme {
                NoraNavGraph(
                    onConnectRequest = { profile ->
                        pendingProfile = profile
                        val intent = VpnService.prepare(this)
                        if(intent != null) {
                            vpnPermission.launch(intent)
                        } else {
                            startForegroundService(Intent(this, com.nora.tunnel.tunnel.TunnelService::class.java).apply {
                                action = "CONNECT"
                                putExtra("profileId", profile.id)
                            })
                        }
                    },
                    onDisconnect = {
                        startService(Intent(this, com.nora.tunnel.tunnel.TunnelService::class.java).apply {
                            action = "DISCONNECT"
                        })
                    }
                )
            }
        }
    }
}
