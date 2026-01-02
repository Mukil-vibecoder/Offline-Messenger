# How to Build WITHOUT Android Studio (Alternative Methods)

If you cannot install Android Studio and do not want to use Appylics, you have to use a **Cloud Compiler**. I have set up the project so you can use **GitHub Actions** (A professional automation tool).

## Option 3: GitHub Actions (Professional & Free)
This method allows "GitHub's Computers" to build the App for you.

1.  **Create a GitHub Account**: Go to [github.com](https://github.com) and sign up (free).
2.  **Create a New Repository**: Name it `OfflineMessenger`.
3.  **Upload Files**:
    -   Upload all the files from `c:\Science Exhibition\OfflineMessenger` to this repository.
    -   *Crucial*: Make sure the `.github` folder (which I just created) is included.
4.  **Wait for Action**:
    -   Once uploaded, click the **"Actions"** tab in your GitHub repository.
    -   You will see a workflow named "Android Build CI" running.
5.  **Download APK**:
    -   When the circle turns green (Success), click on the workflow run.
    -   Scroll down to the **"Artifacts"** section.
    -   Click **offline-messenger-apk** to download your App!

## Option 4: Project IDX (Google's Cloud IDE)
Google has a new full Android Studio that runs **inside your web browser**.

1.  Go to [idx.google.com](https://idx.google.com).
2.  Sign in with Google.
3.  Select "Import a repo" or "Upload files".
4.  You will get a full Android Studio interface in Chrome.
5.  You can see the preview and download the APK from there.

## Option 5: Command Line Tools (Advanced / Hacker Style)
You can install *just* the compiler tools without the heavy visual editor.
1.  Download "Command line tools only" from the Android Developer website.
2.  Install Java (JDK 17).
3.  Use the command terminal to run:
    `gradle assembleDebug`
    *(This is difficult to set up for beginners, so I recommend Option 3 or 4).*
