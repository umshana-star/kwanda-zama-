# Google Play Console Data Safety Form Guidance

Use these exact answers when completing the **Data Safety section** in the Google Play Console for **Zama AI** (`com.aistudio.whatsappagent.zama`).

---

## 1. Overview Questions
| Question | Answer | Details |
|---|---|---|
| **Does your app collect or share any of the required user data types?** | **Yes** | Local message processing and audio for voice-to-text |
| **Is all of the user data collected by your app encrypted in transit?** | **Yes** | All network traffic uses TLS 1.3 / HTTPS |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | In-app "CLEAR DATABASE / HISTORY" button immediately purges all local messages and logs |

---

## 2. Data Types & Declarations

### Category: Messages
- **Data Type**: Other in-app messages (WhatsApp customer inquiries & agent responses)
- **Collected?**: **Yes** (Stored locally on-device in Room SQLite database for business record-keeping)
- **Shared?**: **No** (Not shared with third-party advertisers or data brokers)
- **Is processing ephemeral?**: **No** (Persisted locally for customer relationship history until user clears)
- **Is collection required or optional?**: **Required for core feature** (App function / Account management)
- **Purposes**: App functionality, Customer support

### Category: Audio Files
- **Data Type**: Voice or sound recordings
- **Collected?**: **No** (Audio recordings are processed ephemerally on-device for SpeechRecognizer transcription and never stored as audio files)
- **Shared?**: **No**
- **Is processing ephemeral?**: **Yes**
- **Is collection optional?**: **Optional** (Only triggered if user taps mic button for voice dictation)
- **Purposes**: App functionality (Voice-to-Text)

### Category: Personal Info / Identifiers
- **Data Type**: Phone number / Name of customer contacts in inbound WhatsApp payloads
- **Collected?**: **Yes** (Locally stored in private app sandbox)
- **Shared?**: **No**
- **Purposes**: App functionality (Replying to WhatsApp messages)

### Category: Biometrics
- **Data Type**: Biometric information
- **Collected?**: **No** (Biometric checks use Android system BiometricPrompt; biometric data never leaves the Android OS Secure Enclave / TEE)

---

## 3. Privacy Policy URL
Set the privacy policy URL in Google Play Console to your hosted privacy policy, matching the content in `store_assets/PRIVACY_POLICY.md`.
