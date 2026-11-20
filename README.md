# Die-Cast Racers — Android starter game

A native Kotlin Android prototype for Android 8.0+ (API 26), designed around landscape play, large touch targets, toy-like cars, and short, low-pressure races.

## Included in this project
- Four original starter cars: Ruby Rocket, Blue Bolt, Sunny Truck, Green Flash.
- Garage with car selection.
- Ten named track themes.
- Simple race screen, boost-style GO control, stars, coins, and celebration screen.
- Parent corner with a 3-second hold gate before launching the device camera.
- Basic local custom-car creation from the captured camera thumbnail's average color.
- No ads, analytics, chat, accounts, network calls, or in-app purchases.
- GitHub Actions workflow to build an APK without installing Android Studio locally.

## Important prototype limitations
This is a playable 2D native prototype, not the final 3D production game. The track and car art are drawn with Android Canvas. It does not yet include full 3D physics, animated looping music, voice-over, tilt steering, robust object detection, on-device ML Kit classification, cloud saves, or an in-app purchase system.

The first camera implementation uses Android's camera app and creates a stylized car using the photo thumbnail's average color and a simple body-type heuristic. It does **not** identify logos or accurately reconstruct a photographed die-cast model. Camera behavior varies by device. The game does not upload or retain the photo; it only keeps the generated color/body style in memory for the current app session.

## Build an APK from an Android phone using GitHub
1. Create a GitHub account if needed, then create a new empty repository named `die-cast-racers`.
2. Upload the **contents** of this ZIP (not the ZIP itself) to the repository's root. The `.github/workflows/android.yml` file must be included.
3. Open the repository in Chrome, choose **Actions**, then **Build Die-Cast Racers APK**.
4. If GitHub asks, enable Actions. Tap **Run workflow** (or push to `main` to trigger it automatically).
5. Wait for the run to show a green check. Open that run and find **Artifacts**.
6. Download `Die-Cast-Racers-APK`, extract it, then tap `app-debug.apk` to install. Android may ask you to allow installs from that source.

## Build locally
Requires JDK 17, Android SDK platform 35, and Gradle 8.9 or compatible:
```bash
gradle assembleDebug
```
APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## Next production milestones
1. Replace Canvas art with a proper 3D renderer (Unity or Godot) and optimized low-poly toy assets.
2. Add deterministic toy-physics track movement and accessibility testing with children and caregivers.
3. Add CameraX + on-device object detection / segmentation and a review screen before saving a scanned car.
4. Persist unlocks and scanned cars locally using Room/DataStore.
5. Add parent-adjustable audio controls, original sound design, and voice guidance.
6. Complete privacy review, child-directed app policy review, and store compliance before publishing.
