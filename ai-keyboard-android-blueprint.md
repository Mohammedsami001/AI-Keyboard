# AI Keyboard — Android Blueprint

Copy the code block under **"The Prompt"** into your AI coding assistant
as-is. Everything else here is briefing material — context that'll help
you steer the build and sanity-check what comes out.

---

## Why Android is the platform to nail first

Android is where AI Keyboard's actual differentiator lives. iOS sandboxes
keyboard extensions away from knowing which app is in the foreground —
Android doesn't. That one platform difference is why the per-app tone
rule (WhatsApp gets casual, Gmail gets professional, automatically, with
zero taps) is a real feature on Android and only a manual workaround on
iOS. Get this right here first, prove it works, then port the concept
sideways to iOS with its limitations already understood.

The other reason to start here: Android's inner dev loop for keyboard
development is simply faster. No App Group ceremony, no scary "Allow
Full Access" prompt to fight through in testing, no two-target Xcode
project to keep in sync. You'll iterate on the actual UX — does the
rewrite feel instant, does the tone actually match the app you're in —
several times faster here than on iOS.

---

## The Prompt

```
ROLE
You are building AI Keyboard for Android — a native Kotlin app that is
BOTH a settings/container app AND a custom keyboard
(InputMethodService). This app talks to an already-built backend;
treat its contract as fixed (given in full below). Do not redesign the
backend's endpoints — call them exactly as specified.

PRODUCT CONTEXT
AI Keyboard lets a user type or dictate text, then rewrite it in a
chosen tone using an LLM running server-side. The headline Android
feature: the keyboard detects which app is currently focused (WhatsApp,
Gmail, Slack, etc.) and automatically applies that app's configured
tone rule with zero user interaction — type in WhatsApp, get casual;
switch to Gmail, get professional, same sentence structure, no toggle
to tap. This automatic detection is the reason to build Android first;
it's not possible on iOS at all, so get this exactly right here.

STACK (do not substitute without flagging why)
- Kotlin, no cross-platform framework
- Jetpack Compose for ALL UI — both the container app's screens and the
  keyboard's own UI layer (hosted inside the IME via ComposeView)
- Retrofit + OkHttp for networking, with an OkHttp interceptor handling
  Bearer token attachment and 401-triggered refresh-token retry
- Jetpack DataStore (never SharedPreferences) for settings persistence
  — DataStore is the current recommended approach and handles
  async/Flow-based reads cleanly, which matters since both the
  container app process and the IME process need to read the same
  settings
- Android's built-in SpeechRecognizer for voice input. Do NOT send raw
  audio anywhere. Transcribe on-device via SpeechRecognizer, then send
  the resulting TEXT to the backend's /transcription/correct endpoint
  for tone correction — audio never leaves the device.
- Hilt for dependency injection (keeps the IME, which Android
  instantiates outside your normal Activity lifecycle, cleanly wired to
  the same Retrofit client and DataStore instances the container app
  uses)
- kotlinx.serialization or Moshi for JSON (pick one, be consistent)

PROJECT STRUCTURE — feature-based, not layer-based. Do not create a
top-level "activities/" or "viewmodels/" folder that spans features —
each feature owns its own UI, ViewModel, and logic.

app/src/main/java/com/aikeyboard/app/
  AiKeyboardApplication.kt   (Hilt @HiltAndroidApp entry point)

  auth/
    LoginScreen.kt
    RegisterScreen.kt
    AuthViewModel.kt
    TokenStore.kt              (wraps DataStore, stores access +
                                refresh token, exposes a Flow<Boolean>
                                isLoggedIn state)

  onboarding/
    WelcomeScreen.kt            (3-slide explainer)
    EnableKeyboardScreen.kt     (opens
                                Settings.ACTION_INPUT_METHOD_SETTINGS,
                                polls/re-checks on onResume whether AI
                                Keyboard is now the active/enabled IME)

  toneprofile/
    ToneSetupScreen.kt          (preset picker: Casual/Professional/
                                Friendly/Concise/Formal, optional
                                sample-text calibration box)
    ToneProfileViewModel.kt
    ToneProfileRepository.kt    (PUT/GET /users/me/tone-profile)

  apprules/
    AppRulesScreen.kt           (lists installed apps via
                                PackageManager, lets the user assign a
                                tone override per app)
    AppRulesViewModel.kt
    AppRulesRepository.kt       (GET/PUT/DELETE /users/me/app-rules)
    InstalledAppsProvider.kt    (wraps PackageManager.getInstalledApplications,
                                filters to launchable apps only, fetches
                                labels/icons)

  preview/
    LivePreviewScreen.kt        (a text field + Rewrite button that
                                exercises the exact same
                                /correction/rewrite call the real
                                keyboard uses, so users can test the
                                feature without leaving the app)

  settings/
    SettingsScreen.kt           (sign out, clear local tone cache,
                                privacy policy link, app version)

  keyboard/
    AiInputMethodService.kt     (the InputMethodService itself — owns
                                the lifecycle, hosts the Compose view,
                                talks to InputConnection)
    KeyboardView.kt              (Compose: QWERTY layout, suggestion
                                bar, the "✨ Rewrite" button, mic button)
    KeyboardViewModel.kt         (holds current composed text state,
                                orchestrates rewrite/undo/voice calls —
                                kept separate from AiInputMethodService
                                so the logic is unit-testable without
                                an Android instrumentation test)
    ForegroundAppDetector.kt     (wraps
                                currentInputEditorInfo.packageName —
                                isolate this in its own small class so
                                it's mockable in ViewModel tests)
    VoiceInputController.kt      (wraps SpeechRecognizer, exposes
                                partial + final results as a Flow)

  network/
    ApiClient.kt                 (Retrofit instance, base URL from
                                BuildConfig, not hardcoded)
    AuthInterceptor.kt            (attaches Bearer token, handles
                                401 -> refresh -> retry-once)
    dto/
      RewriteRequest.kt, RewriteResponse.kt, ToneProfileDto.kt,
      AppRuleDto.kt, AuthDto.kt
      (field names and types must match the backend contract below
      exactly — this is the one place a mismatch will silently break
      everything downstream)

  common/
    theme/                       (Compose color scheme, typography)
    components/                  (shared buttons, loading states, error
                                banners — reused across container-app
                                screens and the keyboard's own UI)

BACKEND CONTRACT — already built, call it exactly as specified, do not
invent new fields or reshape responses:

- POST /auth/register  { email, password } -> { accessToken,
  refreshToken, user: { id, email } }
- POST /auth/login  { email, password } -> same shape as register
- POST /auth/refresh  { refreshToken } -> { accessToken, refreshToken }
- GET /users/me/tone-profile -> { presetName, sampleTexts }
- PUT /users/me/tone-profile  { presetName, sampleTexts? } -> same shape
- GET /users/me/app-rules -> [{ id, appPackageName, toneOverride }]
- PUT /users/me/app-rules  { appPackageName, toneOverride } -> upserted
  row
- DELETE /users/me/app-rules/:id -> 204
- POST /correction/rewrite
  { text, tonePreset?, appContext? } -> { correctedText, appliedTone,
  latencyMs }
  Android's job: always populate appContext with the foreground app's
  package name (from ForegroundAppDetector) on every call from the real
  keyboard. Leave tonePreset unset unless the user has explicitly
  overridden it for this one message — let the backend's precedence
  logic (explicit > app rule > profile default) do the resolution work,
  don't duplicate that logic client-side.
- POST /transcription/correct — identical shape, called after
  SpeechRecognizer produces a final transcript
Attach accessToken as a Bearer header on every authenticated call via
AuthInterceptor. On a 401, attempt exactly one refresh-and-retry; if
that also fails, force the user back to LoginScreen and clear TokenStore.

KEYBOARD (InputMethodService) REQUIREMENTS — this is the core deliverable
- Standard QWERTY layout with an autocorrect suggestion bar above it
- On every keystroke that changes focus context (i.e., whenever
  onStartInputView fires with a new EditorInfo), read
  currentInputEditorInfo.packageName via ForegroundAppDetector — this
  is what makes automatic per-app tone possible, do not skip or stub
  this out
- "✨ Rewrite" button: pulls the current composed text via
  InputConnection.getTextBeforeCursor / getTextAfterCursor (concatenate
  sensibly, don't just grab a few characters — get the full reasonably
  available text), calls /correction/rewrite with the resolved
  appContext, and on success replaces the text in-place via
  InputConnection.setComposingText or a delete-then-commit sequence —
  pick whichever is more reliable across the third-party apps you test
  against and document which you chose and why
- "Undo" affordance: appears immediately after a successful rewrite,
  restores the pre-rewrite text from an in-memory snapshot held in
  KeyboardViewModel. One level of undo is enough for v1 — don't build a
  full undo stack.
- Mic button: starts VoiceInputController, shows live partial results
  inline in the suggestion bar area as the user speaks, and on the
  final result calls /transcription/correct then inserts the corrected
  text via InputConnection
- Network failure handling: if /correction/rewrite or
  /transcription/correct fails (timeout, 5xx, no connectivity), show a
  small inline error state in the keyboard UI and leave the user's
  original text completely untouched. Never partially apply a failed
  rewrite, never clear text on error.
- Respect Android's IME lifecycle correctly — onCreateInputView,
  onStartInputView, onFinishInputView — don't leak the
  SpeechRecognizer or hold network calls across lifecycle boundaries;
  cancel in-flight requests in onFinishInputView

CONTAINER APP SCREENS — full list, each backed by the module above
1. Onboarding (3 slides: what it does, the privacy note about not
   storing raw text server-side, a CTA into the enable-keyboard flow)
2. Enable Keyboard — button opens
   Settings.ACTION_INPUT_METHOD_SETTINGS; screen shows a live status
   indicator ("Not enabled" / "Enabled — not default" / "Enabled and
   default") that re-checks via InputMethodManager on every onResume,
   since this is the single biggest drop-off point in any keyboard app
   and the user needs constant, honest feedback about their current
   state
3. Tone Setup — preset picker + optional sample-text box (max 5
   entries, 500 chars each, validated client-side to match the
   backend's limits before the PUT call even fires)
4. Per-App Rules — enumerate installed launchable apps via
   PackageManager, show icon + label + a tone dropdown per app, PUT
   each change immediately (no separate "save" button — treat this like
   a live settings list, not a form)
5. Live Preview — text field + Rewrite button, calls
   /correction/rewrite directly with no appContext (since there's no
   real host app here), so users can sanity-check tone quality before
   trusting the real keyboard
6. Settings — sign out (clears TokenStore), clear local tone cache,
   privacy policy link, app version display

TESTING — required, not optional
- Unit tests (JUnit + MockK or similar) for KeyboardViewModel's
  rewrite/undo state machine — specifically: rewrite success updates
  state and enables undo, rewrite failure leaves original text and
  shows an error state, undo after rewrite restores exactly the
  pre-rewrite snapshot
- Unit tests for AuthInterceptor's 401 -> refresh -> retry-once logic,
  including the case where the refresh itself fails (must force logout,
  not loop)
- Espresso UI test covering the onboarding flow through to the
  Settings.ACTION_INPUT_METHOD_SETTINGS intent firing correctly
- A manual QA checklist in the README, written so a non-engineer could
  follow it:
  1. Install the app, complete onboarding, enable AI Keyboard as the
     active keyboard
  2. Open WhatsApp (or any messaging app), switch to AI Keyboard, type
     a rough sentence, tap Rewrite, confirm the result reads casual
  3. Open Gmail, switch to AI Keyboard, type the same rough sentence,
     tap Rewrite, confirm the result reads noticeably more professional
     than step 2 — this is the single most important thing to verify
     in the entire app
  4. Tap Undo immediately after a rewrite, confirm the original text
     returns exactly
  5. Tap the mic button, speak a sentence, confirm a live partial
     transcript appears, confirm the corrected final text is inserted
  6. Turn on airplane mode, tap Rewrite, confirm an inline error
     appears and the original text is untouched — not cleared, not
     partially replaced
  7. Force-close and reopen the app, confirm login session and tone
     settings persisted

DELIVERABLE
A complete, buildable Android Studio project (Kotlin, Jetpack Compose,
Hilt), a debug APK that installs and runs, a README containing the
7-step manual QA checklist above verbatim, and instructions for
pointing BuildConfig.BASE_URL at a local vs. deployed backend without
editing source code (use build variants or a gradle.properties value,
not a hardcoded string in ApiClient.kt).
```

---

## Battle-tested techniques — what separates a solid build from a fragile one

- **Isolate ForegroundAppDetector in its own tiny class.** It's a
  one-liner wrapping `currentInputEditorInfo.packageName`, but keeping
  it separate from `AiInputMethodService` means `KeyboardViewModel` can
  be unit-tested with a fake detector instead of requiring a real
  Android instrumentation test for logic that has nothing to do with
  Android's IME lifecycle.
- **Let the backend own tone-resolution precedence, fully.** It's
  tempting to have the Android app pre-resolve "well, there's an app
  rule, so I'll just send that as tonePreset" — don't. Send appContext
  raw and let the backend apply the same precedence logic it already
  implements. The moment client-side logic tries to be clever here, it
  will drift from the backend's actual behavior the first time either
  side changes independently.
- **Snapshot text before every rewrite, not just once.** Undo only
  works if the pre-rewrite state is captured fresh on every single
  Rewrite tap, not cached from app launch. Store it in
  `KeyboardViewModel` state, overwritten each time, and treat it as
  disposable — one level of undo, cleanly, beats a half-built undo
  stack that occasionally restores the wrong version.
- **Test the rewrite-and-replace logic against real third-party apps
  early, not just your own preview screen.** `InputConnection` behaves
  differently across WhatsApp, Gmail, Chrome address bars, and Slack —
  some apps handle `setComposingText` gracefully, others need a clean
  delete-then-commit. Budget real device time against multiple popular
  apps before considering the keyboard "done," not just your own Live
  Preview screen which is by definition the easiest possible target.
- **Cancel in-flight network and speech recognition work on
  onFinishInputView.** Users switch apps and dismiss keyboards
  constantly and unpredictably. A rewrite call or a SpeechRecognizer
  session that outlives the view that started it is a guaranteed source
  of crashes and "why did my text change in the wrong app" bug reports.

## Common mistakes to avoid — the ones that bite in week two, not day one

- **Using SharedPreferences instead of DataStore "because it's simpler."**
  It technically works, but DataStore's Flow-based API is what lets
  both the container app process and the IME cleanly observe the same
  settings changes reactively. Retrofitting this later means touching
  every settings read in the codebase.
- **Hardcoding the backend base URL in ApiClient.kt.** You will need a
  local backend for development and a deployed one for testing on a
  real device against real Gemini calls. Wire `BuildConfig.BASE_URL`
  through Gradle build variants from day one — doing it after the fact
  means hunting down every place the URL got copy-pasted.
- **Forgetting that the IME process and the container app's Activity
  process may not share memory the way you'd expect within a single
  app.** Android can and does keep the IME alive independently of
  whether the container app's Activities are running. Don't assume a
  ViewModel scoped to an Activity is available to
  `AiInputMethodService` — this is exactly why Hilt-provided singletons
  backed by DataStore, not in-memory Activity state, are the right way
  to share auth tokens and tone settings across both.
- **Testing Rewrite only against your own Live Preview screen.** It's
  the easiest possible InputConnection target because you control both
  sides. The real test is WhatsApp, Gmail, and whatever chat app your
  actual users live in — each with its own quirks in how it handles
  text replacement from an IME.
- **Treating "enabled" and "set as default" as the same state.** Android
  distinguishes between a keyboard being enabled in settings and being
  the currently active input method — users can complete step one and
  still not actually be using your keyboard. EnableKeyboardScreen's
  status indicator needs to check for both, and the copy needs to
  clearly walk the user through completing the second step, since this
  ambiguity is a well-documented source of "I enabled it but it's not
  working" support tickets across every keyboard app in this category.

---

## Version 2 — what to add once v1 is validated

1. **Learned typing rhythm and autocorrect, not just tone-rewrite on
   demand.** v1's Rewrite button is a deliberate, discrete action. v2
   can add lightweight inline suggestions as the user types — closer to
   how Gboard and SwiftKey operate continuously rather than on a single
   button tap.
2. **Per-app rule suggestions instead of a fully manual list.** Rather
   than making the user manually assign a tone to every installed app,
   ship sensible category-based defaults (messaging apps → Casual,
   email clients → Professional, detected via Android's app category
   metadata where available) and let the user override from there.
3. **Clipboard-aware rewriting.** Let users rewrite text they've copied
   from elsewhere, not only text typed live into a field — useful for
   polishing a paragraph drafted in Notes before pasting it somewhere
   formal.
4. **Widget or quick-settings tile for tone switching**, so a user can
   change their active default tone without opening the app or the
   keyboard's in-line controls — small friction removal that matters
   for a feature used dozens of times a day.
5. **On-device caching of recent corrections**, so a brief network blip
   doesn't force the user to retype a rewrite they already saw succeed
   once — purely a resilience improvement, not a new feature surface.
6. **Multi-window / foldable support testing.** Not urgent for phone-only
   v1, but Android's split-screen and foldable form factors are common
   enough that keyboard behavior across window resizes deserves explicit
   QA before a wider release.

---

## What v1 deliberately does not attempt — and why that's the right call

- **Don't try to read the entire text field with certainty in every
  app.** `InputConnection.getTextBeforeCursor` / `getTextAfterCursor`
  are bounded reads, and some apps restrict what they expose through
  them. Build for "best available context around the cursor," not a
  guaranteed full-document read — this matches what every established
  keyboard app on Android actually does.
- **Don't build a full undo history.** One level of undo (revert the
  last rewrite) covers the overwhelming majority of real usage. A full
  undo stack adds meaningful state-management complexity for a feature
  most users will never reach for.
- **Don't route audio to Gemini.** SpeechRecognizer's on-device (or
  hybrid) transcription is free, fast, and already accurate enough for
  this use case. Sending raw audio to the backend would add latency,
  cost, and a privacy regression with no real accuracy upside at this
  stage.
- **Don't try to auto-detect tone preference without any user input.**
  It's tempting to imagine the app inferring "this user is always
  casual" purely from usage patterns, but that's a real ML/personalization
  project, not a v1 feature — ship explicit presets first, learn from
  real usage data, then consider it for v2's learned-style work (which
  lives in the backend blueprint's roadmap, since that logic belongs
  server-side, not duplicated on-device).
- **Don't chase tablet or Wear OS layouts in v1.** Phone-form-factor
  keyboard UX is the entire game for the MVP; spreading Compose layout
  effort across additional form factors before the core rewrite/tone
  experience is proven would be premature scope.

---

## Smarter alternatives worth knowing about — and why v1 skips them for now

- **CameraX-style ML Kit on-device correction**, instead of a pure
  server round-trip for every rewrite — Google's ML Kit has on-device
  text/language APIs that could handle basic grammar fixes locally.
  Worth evaluating for v2's offline fallback (see the backend
  blueprint's v2 roadmap), but v1 intentionally keeps all AI logic
  server-side so tone quality can be iterated on without shipping app
  updates.
- **WorkManager for deferred voice-correction retry**, instead of a
  simple inline failure state — if a transcription correction fails due
  to a network blip, WorkManager could retry it in the background and
  notify the user once it succeeds, rather than making them redo the
  whole voice input. Reasonable v2 polish; v1's "fail loud, leave text
  untouched" approach is simpler and more predictable to test.
- **A single Activity + Compose Navigation instead of the modular
  screen structure above** — would technically work for something this
  small, but keeping each feature folder self-contained now is what
  makes it realistic to eventually extract, say, the keyboard module
  into its own Gradle module if the team grows, without a rewrite.
- **Firebase Remote Config for tone-preset definitions**, instead of
  the fixed five-preset enum — lets you adjust preset wording or add a
  sixth preset without an app store release. Worth it once you have
  real usage data suggesting the five presets need tuning; premature
  for a v1 whose presets haven't been validated with real users yet.
