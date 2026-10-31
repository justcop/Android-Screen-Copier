# Android Screen Copier

An Android accessibility inspection experiment. This proof of concept reports which labels, UI text, descriptions, view IDs, and positions are exposed by the currently displayed screen of a **user-selected** app.

## Privacy and limitations

- A probe only starts when you explicitly tap **Start two-minute probe**.
- The service checks the selected app's package identifier before collecting a view hierarchy.
- No screenshot capture, screen recording, root access, automatic tapping, internet permission, or cloud upload.
- Password fields, editable fields and unsent drafts are deliberately excluded.
- The last result is stored privately on the device. Use **Delete local result** to remove it.
- The report may contain private text belonging to other people, so do not share the full report unredacted.
- This tool cannot inspect messages that the target app has not loaded into its visible accessibility tree.

## Build a debug APK with GitHub Actions

Every push to \`main\` or manual workflow dispatch triggers **Build Android APK** in the **Actions** tab.

1. Open [Build Android APK](../../actions/workflows/build-android.yml).
2. Open a successful workflow run, then download the artifact **android-ui-probe-debug-apk**.
3. Extract the downloaded archive and install \`app-debug.apk\` on your Android phone.

It is a debug-signed, installable APK, not a Play Store release. The artifact is retained for seven days.

The build uses JDK 17, Gradle 8.9, Android API 35 and Android Gradle Plugin 8.7.3.

## Run the test

1. Open **UI Probe** and enter the Android package identifier for the app you want to inspect.
2. Tap **Save target app**.
3. Tap **Open Accessibility settings** and enable the UI Probe accessibility service.
4. Return to UI Probe and tap **Start two-minute probe**.
5. Switch to your selected app and scroll through the screen you want to inspect.
6. Return to UI Probe and tap **Refresh result**.
7. The detailed report shows elements present in the current UI hierarchy. Use **Copy anonymised counts only** to share the numerical summary without copying message text.
8. Delete the result and disable Accessibility access when finished.

Recent Android versions can block accessibility services for sideloaded APKs until you explicitly select **Allow restricted settings** in Android Settings > Apps > UI Probe > the three-dot menu. Only grant this after reviewing the app's source.

## Reading the result

- **Text entries appear:** The target app exposes those specific pieces of content through Accessibility.
- **Only navigation labels appear:** The app may hide message content, draw it without semantic nodes, or expose only some elements.
- **No result:** Check the package identifier, accessibility service, session time, and whether the target generated a window-change or scroll event.

This test does not yet implement archiving, sender attribution, deduplication, scrolling back through older conversations, timestamps, or image capture. Screenshot protection and accessibility are separate Android mechanisms; this app does not bypass screenshot protection.

## Local build

Open this directory in Android Studio, or run \`gradle :app:assembleDebug\` with Gradle 8.9 and Android SDK 35 installed. The GitHub Actions workflow installs these dependencies in CI.
