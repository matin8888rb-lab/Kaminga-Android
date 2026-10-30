# Kaminga Android

A native Android comic-reader prototype built with Kotlin and Jetpack Compose.

## Implemented in the current prototype
- Light blue / mint-green interface and launcher icon.
- Home screen with horizontal content rails.
- Search by title, English title, and genre, plus genre filters.
- Comic detail screens and selectable demo chapters.
- Local saved-library state.
- Local reading chapter progress and a “continue reading” action.
- A reader with page-by-page and continuous text layouts.
- Android back navigation between reader, details, tabs, and home.
- Local storage of demo text previews for offline testing; downloaded previews appear in the library.

## Important limitations
- Titles, covers, and chapter pages are placeholders/demo content, not real licensed comics.
- Offline saving currently stores demo text previews only, not real comic image/PDF files.
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
