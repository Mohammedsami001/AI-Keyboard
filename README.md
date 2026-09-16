# AI Keyboard

An AI-powered Android keyboard that detects the active app and automatically applies tone rules for rewritten text.

## QA Checklist

1. Install the app, complete onboarding, enable AI Keyboard as the active keyboard
2. Open WhatsApp (or any messaging app), switch to AI Keyboard, type a rough sentence, tap Rewrite, confirm the result reads casual
3. Open Gmail, switch to AI Keyboard, type the same rough sentence, tap Rewrite, confirm the result reads noticeably more professional than step 2 — this is the single most important thing to verify in the entire app
4. Tap Undo immediately after a rewrite, confirm the original text returns exactly
5. Tap the mic button, speak a sentence, confirm a live partial transcript appears, confirm the corrected final text is inserted
6. Turn on airplane mode, tap Rewrite, confirm an inline error appears and the original text is untouched — not cleared, not partially replaced
7. Force-close and reopen the app, confirm login session and tone settings persisted

## Environment Variables
The application reads the base URL from the `BuildConfig.BASE_URL` value.
In `app/build.gradle.kts`, `BASE_URL` is set to different values depending on the build type (release vs debug). You can modify it there.
