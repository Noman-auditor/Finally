package com.nora.tunnel.core.model

enum class Protocol {
    VLESS,
    VMESS,
    TROJAN,
    SHADOWSOCKS,
    WIREGUARD,
    OPENVPN,
    IKEV2,
    SSH,
    SOCKS,
    HTTP,
    HYSTERIA2,
    TUIC
}

enum class Core {
    XRAY,
    SINGBOX,
    WIREGUARD,
    OPENVPN,
    IKEV2,
    SSH
}

enum class Transport {
    TCP,
    UDP,
    TLS,
    WS,
    GRPC,
    H2,
    QUIC,
    H3
}

enum class Security {
    NONE,
    TLS,
    REALITY,
    UTLS
}
