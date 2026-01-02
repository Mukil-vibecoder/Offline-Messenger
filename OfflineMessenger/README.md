# Offline Bluetooth Emergency Messenger

A Zero-Cost, 100% Offline Android Application for the Science / Innovation Competition.
This app enables secure, one-to-one communication between devices using Bluetooth Classic without any Internet or SIM card.

## 📁 Project Structure
The project is a standard Android Studio project structure:
- `app/src/main/java/com/example/emergencymessenger`: Contains all Kotlin source code (Logic).
- `app/src/main/res/layout`: Contains all UI screens (XML).
- `app/src/main/AndroidManifest.xml`: Permissions and App settings.

## 🚀 How to Build APK (For Free)
Since you are using **Appylics** (or similar online builders) to generate the APK:

1.  **Locate Project Folder**: Go to your computer folder where this code is saved: `c:\Science Exhibition\OfflineMessenger`.
2.  **ZIP the Project**:
    -   Select the **OfflineMessenger** folder (the root folder).
    -   Right-click -> **Send to** -> **Compressed (zipped) folder**.
    -   Name it `OfflineMessenger_Project.zip`.
3.  **Upload**:
    -   Go to the Appylics website.
    -   Select "Upload Project".
    -   Upload your `OfflineMessenger_Project.zip`.
    -   Wait for the build to complete and download your APK.

## 📱 How to Use the App
1.  **Install**: Install the APK on at least two Android phones (Android 10 or higher).
2.  **Permissions**: Open the app and **Allow** all permissions (Location, Bluetooth).
    -   *Note: Location is required by Android to find nearby Bluetooth devices.*
3.  **Scan**:
    -   Tap **Scan for Neighbors** on one phone.
    -   Tap **Make Discoverable** on the other phone (if not already visible).
4.  **Connect**:
    -   Select the device name from the list.
    -   Wait for "Connected to [Name]" message.
5.  **Chat**:
    -   Type a message and tap **Send**.
    -   Tap **SOS** for emergency alert.
6.  **Forwarding (Manual)**:
    -   Long-press any message in the chat.
    -   Select **Yes** to forward.
    -   The app will disconnect and let you choose a new device to send the message to.

## 🧠 Technical Explanation (For Judges)

### 1. Bluetooth Connection Logic
The app uses **Bluetooth Classic (RFCOMM)** which works like a virtual serial cable through the air.
-   **Server (AcceptThread)**: Every phone constantly listens for incoming connections.
-   **Client (ConnectThread)**: When you tap a device, your phone initiates valid connection.
-   **Data Transfer (ConnectedThread)**: Once linked, both phones open Input/Output streams to send text bytes instantly.

### 2. Manual Forwarding Logic (Safety Feature)
To prevent "Message Flooding" (which can crash networks), we implemented **Strict Manual Forwarding**:
-   The app **NEVER** automatically rebroadcasts messages.
-   Users must read a message, decide it is important, and **Manually Forward** it.
-   The app forces a disconnect from the current user before connecting to the next, ensuring a deliberate action chain.

## ⚠️ Common Errors & Fixes
| Error | Fix |
| :--- | :--- |
| **"No devices found"** | Ensure the other phone is "Discoverable" (Visible to new devices) in its Bluetooth settings or click "Make Discoverable" in the app. |
| **"Connection Failed"** | Move closer (within 10 meters). Turn Bluetooth OFF and ON again on both phones. |
| **"Permission Denied"** | Go to Android Settings -> Apps -> Offline Messenger -> Permissions -> Allow Location & "Nearby Devices". |

## 🛡️ Safety & Ethics
-   **No Internet**: Check `AndroidManifest.xml` - absolutely no `android.permission.INTERNET`.
-   **No Ads/Tracking**: No Google Play Services or AdMob SDKs included.
-   **Privacy**: Messages are stored only in temporary RAM and vanish when the app closes.

---
**Good Luck with your Science Exhibition!**
