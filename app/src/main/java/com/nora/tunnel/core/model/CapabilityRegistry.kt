package com.nora.tunnel.core.model

data class CoreCapability(
    val core: Core,
    val protocols: Set<Protocol>,
    val transports: Set<Transport>
)

object CapabilityRegistry {
    val registry: Map<Core, CoreCapability> = mapOf(
        Core.XRAY to CoreCapability(
            Core.XRAY,
            setOf(Protocol.VLESS, Protocol.VMESS, Protocol.TROJAN, Protocol.SHADOWSOCKS),
            setOf(Transport.TCP, Transport.WS, Transport.GRPC, Transport.QUIC, Transport.TLS)
        ),
        Core.SINGBOX to CoreCapability(
            Core.SINGBOX,
            setOf(
                Protocol.VLESS,
                Protocol.VMESS,
                Protocol.TROJAN,
                Protocol.SHADOWSOCKS,
                Protocol.HYSTERIA2,
                Protocol.TUIC
            ),
            setOf(Transport.TCP, Transport.UDP, Transport.WS, Transport.QUIC)
        ),
        Core.WIREGUARD to CoreCapability(
            Core.WIREGUARD,
            setOf(Protocol.WIREGUARD),
            setOf(Transport.UDP)
        ),
        Core.OPENVPN to CoreCapability(
            Core.OPENVPN,
            setOf(Protocol.OPENVPN),
            setOf(Transport.TCP, Transport.UDP)
        ),
        Core.IKEV2 to CoreCapability(
            Core.IKEV2,
            setOf(Protocol.IKEV2),
            setOf(Transport.UDP, Transport.TCP)
        ),
        Core.SSH to CoreCapability(
            Core.SSH,
            setOf(Protocol.SSH),
            setOf(Transport.TCP)
        )
    )

    fun isValid(core: Core, protocol: Protocol, transport: Transport): Boolean {
        val cap = registry[core] ?: return false
        return protocol in cap.protocols && transport in cap.transports
    }
}
