# ATNA Privacy Policy and Terms of Use

ATNA is a fork of [Amethyst](https://github.com/vitorpamplona/amethyst). This privacy policy extends Amethyst's original policy with ATNA-specific additions.

## Privacy Policy

Effective as of Feb 11, 2026

The ATNA app does not collect or process any personal information from its users.

The app is used to browse third-party Nostr servers (called Relays) that may or may not collect personal information and are not covered by this privacy policy. Each third-party relay server comes equipped with its own privacy policy and terms of use that can be viewed through the app or through that server's website. The developers of this open-source project or maintainers of the distribution channels (app stores) do not have access to the data located in the user's device. Accounts are fully maintained by the user. We do not have control over them.

The app may collect a per-device token, your public key, and a preferred Relay to connect to and provide push notification services through Google's Firebase Cloud Messaging (Android only). Other than that, the data from connected accounts is only stored locally on the device when it's required for the functionality and performance of ATNA. This data is strictly confidential and cannot be accessed by other apps (on non-rooted devices). Data can be deleted by clearing ATNA's local storage or uninstalling the app.

### Local Storage (LMDB)

ATNA stores Nostr events in a local LMDB database on your device. This data never leaves your device and is used to improve startup time and reduce relay traffic. The database has a 4 GB size cap and automatically prunes old data. You can clear it by deleting ATNA's application data.

### Marmot Encrypted DMs

ATNA supports Marmot group messaging, which uses the MLS protocol (RFC 9420) for end-to-end encryption. Message content is encrypted and only readable by group members. However:

- **Key packages** (kind 443) are published to relays and are publicly visible. They contain your public key and MLS key material needed for others to invite you to groups.
- **Welcome messages** (kind 444) and **group messages** (kind 445) are encrypted. Only group members can read the content.
- **Relay metadata** about when you send and receive Marmot messages is visible to the relays you connect to.

### Privacy with Relay services

Your Internet Protocol (IP) address is exposed to the relays you connect to. If you want to improve your privacy, consider utilizing a service that masks your IP address (e.g., a VPN) from trackers online.

The relay can also see which public keys you are using and what information you are requesting from the network. Your public key is tied to your IP address and your relay filters.

Relays have your data in raw text (except for encrypted content). They know your IP, your name, your location (guessed from IP), your pub key, all your contacts, and other relays, and can read every action you do (post, like, boost, quote, report, etc) with the exception of the content inside Private Zaps, Private DMs, and Marmot encrypted messages.

### Bug Reporter

ATNA includes an optional bug reporter. If you choose to submit a bug report or crash report, the following information is sent to GitHub:

- Device/OS information
- App version
- Crash stack trace (if applicable)
- Any description you provide

Bug reports are only submitted with your explicit action. No data is sent automatically.

### Visibility & Permanence of Your Content on Nostr Relays

#### Information Visibility

Content that you share can be shared with other relays by any user of the network.
The information you share is publicly visible to anyone reading from relays that have access to your information. Your information may also be visible to Nostr users who do not share relays with you.

#### Information Permanence

Information shared on Nostr should be assumed permanent for privacy purposes. There is no way to guarantee deleting or editing any content once posted.

## Terms of Use

ATNA is open-source software provided as-is under the MIT License. Use at your own risk.

## Other Notes

We reserve the right to modify this Privacy Policy at any time. Any modifications to this document will be effective upon posting of the new terms.

For questions, open an issue at [github.com/HalfzwareLinda/atna](https://github.com/HalfzwareLinda/atna/issues).
