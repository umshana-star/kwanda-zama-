# Privacy Policy for Zama AI

**Effective Date:** October 8, 2026  
**Last Updated:** October 8, 2026  
**Contact:** privacy@zama-ai.app  

Welcome to **Zama AI** ("we", "our", or "the App"). We respect your privacy and are committed to protecting personal data collected or processed through the application. This Privacy Policy details our data collection, usage, and security practices in strict adherence to the Google Play Developer Program Policies.

---

### 1. Information We Collect and Process

#### A. Biometric Data (Local Only)
- The app offers biometric authentication (fingerprint and face unlocking) to safeguard access to customer conversation logs.
- **Biometric verification is handled strictly on-device by the Android operating system (`BiometricPrompt` / Hardware-Backed KeyStore).**
- **We never collect, store, transmit, or have access to your biometric samples or raw fingerprint/facial templates.**

#### B. Audio & Microphone Access (Optional & User-Initiated)
- The app requests `RECORD_AUDIO` permission exclusively to support on-demand voice-to-text dictation via the Android `SpeechRecognizer` API.
- Microphone input is only captured when the user explicitly taps the microphone button.
- Audio recordings are processed ephemerally for transcription and are **never uploaded to external servers, sold, or stored as raw audio files**.

#### C. Inbound WhatsApp Business Messages & Webhook Payloads
- To deliver autonomous concierge services, the app processes incoming customer chat messages, phone numbers, and inquiry contents provided via configured WhatsApp Business API webhooks.
- Messages are persisted locally on your device within an encrypted Room SQLite database for your business records and auditing.

#### D. Network and Diagnostics
- The app utilizes network access (`INTERNET`, `ACCESS_NETWORK_STATE`) to communicate with the WhatsApp Business Cloud API and secure proxy endpoints.
- No third-party ad tracking, fingerprinting, or unsolicited analytics SDKs are embedded in the app.

---

### 2. How We Use Information
We use the information processed solely to:
1. Provide autonomous customer support and booking responses.
2. Maintain local audit logs of interactions and agent decisions.
3. Allow users to export and backup their interaction history in standard JSON format.
4. Secure app access through local biometric authentication and salted PIN encryption.

---

### 3. Data Storage, Security, and Retention
- **Local Persistence**: Conversation history is stored locally in your device's private app sandbox using Room Database.
- **Encryption**: Sensitive credentials and salted PIN verification hashes are secured using Android KeyStore cryptography (AES-256-GCM / PBKDF2).
- **Data Deletion**: Users may clear all local message history and logs directly within the app console at any time. Uninstalling the application completely removes all locally stored data.

---

### 4. Third-Party Disclosures
We do not sell, rent, or trade your personal or customer data to third parties. API communication is conducted directly with configured enterprise endpoints (Meta WhatsApp Business API and secure Google Gemini proxy endpoints).

---

### 5. Children's Privacy
Zama AI is a business productivity application designed for commercial salon and service management. We do not knowingly collect personal information from children under 13.

---

### 6. Changes to this Policy
We may update this Privacy Policy from time to time. Updates will be reflected in the App and on our distribution listings.

For privacy inquiries, please contact: `kwandazama01@gmail.com`.
