# Ride Auto Accept

Android accessibility-service app that taps the "Accept" button in up to two driver apps
(for example Ola and Uber), only when the offer matches that app's minimum fare / maximum
pickup distance.

## Build the APK (Android Studio)
1. Open this folder in Android Studio (File -> Open). Let Gradle sync finish.
2. Build -> Build Bundle(s) / APK(s) -> Build APK(s).
3. APK: app/build/outputs/apk/debug/app-debug.apk

## Build the APK (no Android Studio, using GitHub)
1. Create a free GitHub account and a new repository.
2. Upload everything in this folder, including the hidden .github folder.
3. Open the Actions tab -> "Build APK" -> Run workflow.
4. When it finishes, open the run and download "RideAutoAccept-apk" at the bottom.
   Unzip it to get app-debug.apk.

## Set up on the phone
1. Install the APK (allow "install unknown apps").
2. Open the app. Fill App 1 and App 2 with each driver app's package name.
   Easy way to find a package name: open the driver app's Google Play page in a browser;
   the address ends with id=<package name>.
3. Android 13+: Settings -> Apps -> Ride Auto Accept -> three-dot menu -> "Allow restricted settings".
4. Settings -> Accessibility -> Ride Auto Accept -> turn ON.
5. Switch "Auto-accept ON" in the app.

## Finding the Accept button
Use Android Studio's Layout Inspector (or `adb shell uiautomator dump`) on the driver app's
offer screen to read the button's text or view ID, and enter it for that app.
