# Ride Auto Accept

Android accessibility-service app that taps the "Accept" button of a driver app you choose,
when the offer matches your minimum fare / maximum pickup distance.

## Build the APK
1. Install Android Studio and open this folder (File -> Open). Let Gradle sync finish.
2. Build -> Build Bundle(s) / APK(s) -> Build APK(s).
3. APK: app/build/outputs/apk/debug/app-debug.apk
   (For a shareable release APK use Build -> Generate Signed Bundle / APK -> APK.)

## Set up on the phone
1. Install the APK (allow "install unknown apps" for your browser/file manager).
2. Open the app, enter the driver app's package name, save.
   Find it with: adb shell pm list packages | grep <name>
3. Android 13+: Settings -> Apps -> Ride Auto Accept -> three-dot menu -> "Allow restricted settings".
4. Settings -> Accessibility -> Ride Auto Accept -> turn ON.
5. Switch "Auto-accept ON" in the app.

## Finding the Accept button
Use Android Studio's Layout Inspector (or `adb shell uiautomator dump`) on the driver app's
offer screen to read the button's text or view ID, and enter it in the app.
