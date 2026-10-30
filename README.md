# Kaminga Android

A native Android comic-reader prototype built with Kotlin and Jetpack Compose.

## Current prototype features
- Light blue / mint-green interface and launcher icon.
- Home screen with horizontal content rails and a recently opened / continue-reading rail.
- Search by Persian title, English title, and genre; genre filters, result count, and clear-filter action.
- Comic detail pages with scrollable chapter lists.
- Local saved-library state.
- Local chapter and page progress, plus a “continue reading” action.
- Reader with page-by-page and continuous layouts.
- Import multiple image pages from Android's document picker and copy them into app-private storage.
- Render imported image pages in both reader layouts; text demo remains as fallback.
- Local storage of demo text previews for offline testing; previews appear in the library.
- Android back navigation between reader, details, tabs, and home.

## Important limitations
- Built-in titles, covers, and chapter text are demo placeholders, not real licensed comics.
- Imported images are stored locally and can be read offline, but the current prototype treats the selected image set as one reading sequence per title; per-chapter image organization is not yet implemented.
- The separate “offline preview” action stores demo text, not a remote comic download.
- Accounts, secure backend, cross-device sync, real authentication, and SMS verification are not implemented.
- This is a debug/test prototype, not a release-ready public app.
- A successful APK build does not prove every feature works on a physical phone; install and test the generated APK.

## Build
Use JDK 17 and Gradle 8.9, then run:

```sh
gradle assembleDebug
```

Or use the GitHub Actions workflow to build a debug APK.

Package: `com.kaminga.reader`
