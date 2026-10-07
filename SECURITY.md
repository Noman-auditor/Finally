# Security — NORA TUNNEL

NORA TUNNEL is designed with a security-first approach for authorized VPN and tunnel configurations.

## Security Principles

### 1. No Custom Cryptography

NORA TUNNEL must not implement its own cryptographic algorithms.

Use established and reviewed implementations provided by appropriate upstream projects, such as:

- Xray
- sing-box
- WireGuard
- OpenVPN

Cryptographic primitives should not be replaced with custom implementations.

## 2. Untrusted Configuration Validation

Imported tunnel configurations must be treated as untrusted input.

Before a configuration is used:

1. Parse the input.
2. Validate the structure.
3. Validate required fields.
4. Validate protocol/core compatibility.
5. Validate transport compatibility.
6. Validate server address and port.
7. Normalize supported values.
8. Reject malformed or unsupported configurations.
9. Only then prepare the tunnel.

The application must not blindly execute arbitrary imported configuration data.

## 3. Command Injection Protection

Tunnel configuration values must never be concatenated into shell commands.

Avoid patterns such as:

```text
sh -c "command $userInput"
