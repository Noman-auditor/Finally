# Privacy Policy — NORA TUNNEL

**NORA TUNNEL — Secure. Private. Connected.**

## Overview

NORA TUNNEL is designed as a local-first Android VPN client.

The application does not require a remote account for its core local functionality.

## Data Storage

### Tunnel Configurations

Tunnel profile metadata is stored locally using:

- Android Room Database
- Local application storage

Examples of profile metadata include:

- Profile name
- Protocol
- Core
- Transport
- Server address
- Server port
- TLS preference
- Favorite/enabled state

### Credentials and Secrets

Sensitive credentials are not stored in ordinary database fields.

Where applicable, sensitive values are stored using:

- Android Keystore-backed encryption
- EncryptedSharedPreferences

Examples include:

- Passwords
- Tokens
- Private keys
- Authentication secrets

Sensitive values must not be written to normal logs.

## History and Logs

Connection history and diagnostic information are stored locally by default.

NORA TUNNEL does not silently upload:

- Connection history
- VPN logs
- Diagnostic reports
- Tunnel configurations
- Credentials
- Private keys

## Telemetry

Telemetry is disabled by default.

If telemetry is introduced in a future version, it must be:

1. Clearly disclosed.
2. Disabled by default unless appropriate consent is obtained.
3. Explicitly opt-in where required.
4. Limited to the minimum necessary information.

## Export and Backup

Export is user initiated.

When the application provides an export/share operation, the user must explicitly choose the destination through Android's Storage Access Framework (SAF) or an Android share action.

The application must not silently upload or transmit exported data.

## QR and Backup Security

QR/profile exports should contain only the minimum profile information required for configuration transfer.

Sensitive credentials should not be included by default.

Backup files should not contain:

- Passwords
- Private keys
- Access tokens
- Refresh tokens
- Authentication headers
- Other sensitive credentials

Users may need to re-enter sensitive credentials after importing a profile.

## Network Traffic

NORA TUNNEL is a VPN client. Network traffic may be processed by the configured VPN server and selected tunnel core.

The application itself should not claim that traffic is private or anonymous merely because a VPN interface has been created.

A connection should only be reported as fully connected after the configured tunnel/core has successfully established the intended connection.

## Permissions

NORA TUNNEL requests only permissions required for its functionality.

Examples include:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- VPN service permissions required by Android
- Foreground service permissions required for the VPN service
- Notification permission where required by Android

The application should avoid unnecessary permissions.

## Third-Party Services

The application is designed to work without requiring a remote user account.

Tunnel cores and protocol implementations may have their own software licenses, dependencies, and security considerations. Those components should be reviewed independently.

## User Control

Users control:

- Which tunnel profile is configured
- Which server is used
- When the VPN connection is started
- When the VPN connection is stopped
- Which configuration is exported
- Which diagnostic information is shared

## Privacy by Design

NORA TUNNEL follows these principles:

- Local-first storage
- Minimum required permissions
- No silent uploads
- No credentials in normal logs
- Explicit user-initiated export
- Secure storage for sensitive values
- No unnecessary telemetry
- No fake connection or network statistics

## Security Contact

Security issues should be reported through the project's designated security contact or repository security process.

---

**Last updated:** 2026
