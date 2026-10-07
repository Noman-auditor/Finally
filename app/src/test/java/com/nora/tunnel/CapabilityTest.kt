package com.nora.tunnel

import com.nora.tunnel.core.model.CapabilityRegistry
import com.nora.tunnel.core.model.Core
import com.nora.tunnel.core.model.Protocol
import com.nora.tunnel.core.model.Transport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityTest {
    @Test
    fun validCombo() {
        assertTrue(CapabilityRegistry.isValid(Core.XRAY, Protocol.VLESS, Transport.WS))
        assertTrue(CapabilityRegistry.isValid(Core.SINGBOX, Protocol.HYSTERIA2, Transport.QUIC))
        assertTrue(CapabilityRegistry.isValid(Core.OPENVPN, Protocol.OPENVPN, Transport.TCP))
        assertTrue(CapabilityRegistry.isValid(Core.IKEV2, Protocol.IKEV2, Transport.UDP))
        assertTrue(CapabilityRegistry.isValid(Core.SSH, Protocol.SSH, Transport.TCP))
    }

    @Test
    fun invalidCombo() {
        assertFalse(CapabilityRegistry.isValid(Core.XRAY, Protocol.OPENVPN, Transport.TCP))
        assertFalse(CapabilityRegistry.isValid(Core.WIREGUARD, Protocol.VLESS, Transport.UDP))
        assertFalse(CapabilityRegistry.isValid(Core.SSH, Protocol.IKEV2, Transport.TCP))
    }
}
