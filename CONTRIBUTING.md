# Contributing to Downloader

Thank you for your interest in contributing to **Downloader**. As an open-source project, community contributions, bug reports, and suggestions help make this tool better for everyone.

Please review the guidelines below to ensure a smooth contribution and review process.

---

## Branching Strategy & Workflow

This repository uses a two-branch workflow:

- **`develop` (Default Working Branch)**:
  All ongoing development, feature branches, and community Pull Requests **must** branch off and target `develop`.
- **`main` (Production Release Branch)**:
  `main` reflects the latest stable production release. Merges into `main` occur exclusively from `develop` and are accompanied by a version release tag (`vX.Y.Z`).

```
(main)    v1.0.0 ───────────────────────────────────────── v1.0.1 (Release)
                   ▲                                        ▲
                   │                                        │
(develop) ─────────┴─────●───────────●──────────●───────────┴────────►
                         ▲           ▲          ▲
                         │           │          │
(feature/fix)   feature/xyz      fix/abc    docs/update
```

### Creating a Branch
1. Ensure your local `develop` branch is up-to-date:
   ```bash
   git checkout develop
   git pull origin develop
   ```
2. Create your topic branch:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```

---

## Development Environment & Prerequisites

Ensure your workstation meets the following requirements:

- **Java Development Kit (JDK)**: Version 21 (OpenJDK / Eclipse Temurin or Android Studio bundled runtime).
- **Android Studio**: Android Studio Koala, Ladybug, or newer.
- **Android SDK**:
  - `compileSdk`: 37
  - `targetSdk`: 37
  - `minSdk`: 27 (Android 8.1 Oreo)
- **NDK & ABIs**: Native components (`yt-dlp`, Python, FFmpeg, Aria2c) require NDK support for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.

---

## Building & Testing Locally

Before opening a pull request, verify that your changes compile cleanly and pass all unit tests:

```bash
# 1. Verify Kotlin compilation
./gradlew compileDebugKotlin

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Assemble Debug APK
./gradlew assembleDebug

# 4. Verify Release Compilation & R8 Shrinking
./gradlew assembleRelease
```

---

## Code Style & Architecture Guidelines

1. **Jetpack Compose Idioms**:
   - Keep Composables stateless where practical and hoist state to ViewModels or state holders.
   - Use `remember` and `derivedStateOf` for derived values to prevent unnecessary recompositions.
   - Follow Material 3 theming guidelines defined in `com.leanwell.downloader.ui.theme`.

2. **Clean Comment Hygiene**:
   - Write clear, self-explanatory code. Avoid redundant restatements.
   - Preserve genuine engineering comments, caveats, external format quirks, or thread-safety explanations.

3. **Privacy & Security**:
   - Sensitive user data (such as session cookies) must always be encrypted with the **Android Keystore** hardware-backed provider (`HardwareEncryptedCookieStore`).
   - Never commit sensitive configuration files or personal credentials. Local development secrets belong in `.env` (which is excluded by `.gitignore`).

4. **ProGuard / R8 Rules**:
   - If adding a new third-party library that uses reflection, JNI, or dynamic class loading, ensure corresponding preservation rules are added to `app/proguard-rules.pro`.

---

## Commit Conventions

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `feat:` A new feature or capability
- `fix:` A bug fix
- `docs:` Documentation updates (`README.md`, `CONTRIBUTING.md`, comments)
- `refactor:` Code changes that neither fix a bug nor add a feature
- `perf:` Performance improvements
- `chore:` Dependency bumps, build script changes, or workflow maintenance

Example:
```bash
git commit -m "feat: add support for custom user-agent in download options"
```

---

## Submitting a Pull Request

1. **Push your branch**:
   ```bash
   git push origin feature/your-feature-name
   ```
2. **Open a Pull Request**:
   - Target the **`develop`** branch (do not target `main` directly).
   - Provide a clear, descriptive title and summarize the motivation and changes in the PR body.
   - Attach screenshots or screen recordings for UI changes.
3. **Review & Approval**:
   - Repository maintainers will review your submission.
   - Once all automated checks pass and approvals are granted, your branch will be merged into `develop`.

---

## Reporting Bugs & Feature Requests

- **Bug Reports**: Please include your device model, Android OS version, app version, the target URL (if applicable), and steps to reproduce. If possible, attach relevant `adb logcat` output.
- **Feature Requests**: Outline the proposed feature, explain why it would be beneficial, and provide mockups or examples where applicable.
