# NORA TUNNEL — implementation status

Native Android (Kotlin + Compose UI, Java VpnService + sing-box/libbox data plane).
**Not yet built or tested on a device.** The code in this revision was written without a compiler available.

## Works in code (needs on-device verification)
- Profiles: import from file or clipboard (vless, vmess, trojan, ss, hysteria2/hy2, tuic, socks, http links; base64
  subscriptions; WireGuard .conf; sing-box JSON outbound or full config). Each profile is validated at import.
- Storage: all profiles live in one AES-256-GCM blob keyed by the Android Keystore. Legacy plaintext stores are migrated
  and wiped. Backups are disabled. Export is an explicit, warned action.
- Connect: the selected local profile is converted to a sing-box (1.12+ schema) config and handed to the core.
  If no local profile is selected, the original control-plane flow is used unchanged.
- Settings that are actually read by the service / config builder:
  auto-connect, connect on boot, kill switch, bypass LAN, tunnel IPv6 (off = blocked, not leaked), per-app routing
  (allow-list or deny-list picker), DNS server + DoH, strict validation, log redaction.
- Kill switch: if the core fails to start or stops unexpectedly the service swaps in an empty TUN that blocks all
  traffic until you tap Disconnect. For protection while the app is not running use Android's Always-on VPN +
  "Block connections without VPN" (button in Settings).

## Still required before shipping
1. `app/libs/libbox.aar` — run `scripts/build-libbox.sh`. The config schema assumes libbox >= 1.12.
2. `gradle/wrapper/gradle-wrapper.jar` — `scripts/build-apk.sh` regenerates it if Gradle is installed.
3. Compile, fix any errors, install on an ARM device, test each protocol with authorised servers.
4. Release signing: set NORA_KEYSTORE, NORA_KEYSTORE_PASSWORD, NORA_KEY_ALIAS, NORA_KEY_PASSWORD.
5. Control-plane URL must be https (cleartext traffic is now disabled).

## Not implemented (removed from the UI rather than faked)
Auto-reconnect, speed test, diagnostics, traffic history, themes, notification preferences, OpenVPN/IKEv2/SSH
(sing-box does not provide them), Shadowsocks plugins, TCP HTTP-header obfuscation.
