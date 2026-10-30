# Handoff: a DPI per screen size

> Working notes for this branch, not part of the feature. Delete this file before the branch is
> opened upstream.

## What the change is

The density Android Auto is told about used to be a single value for the whole device:
`Settings.dpiPixelDensity`, with `0` meaning Automatic. On a device that has two screens of its
own — a foldable's cover and inner display — a density that suits one is wrong on the other, and
there was no way to keep both.

This branch stores a DPI per screen size and picks it at connect time from the panel the
projection is about to draw into.

## The design decision, and the alternatives rejected

**Chosen: key the stored density to the panel size.** The projection already reads the panel it is
about to draw into (`physicalWidthPx/HeightPx`, set in `HeadUnitScreenConfig.init()`), and two fold
states are different pixel sizes by definition, so that reading alone tells them apart.

Rejected: **fold detection** (`androidx.window` / `WindowInfoRepository` / `FoldingFeature`).

- It adds a dependency and a lifecycle for no information the panel reading does not already carry.
- It only helps foldables, while a panel key also covers DeX, an external display, a tablet in a
  keyboard case — anything with more than one screen size.
- The density is announced once, inside the service discovery video config, i.e. at connect; a
  live fold signal could not be used at announce time anyway.

Rejected: **a threshold heuristic** ("folded if the panel is narrower than N px"). It needs a magic
constant per device and it breaks on rotation.

## Data

- New pref `dpi-screen-profiles`, one string: `1812x2176=420;968x2376=300`.
- The key is normalised (`min × max`), so rotating the device does not create a second entry.
- `dpi-pixel-density` stays as the shared value: it is what any screen without its own entry uses,
  and it is still what older exports carry.

Matching (`DensityScreenProfile.select`): exact normalised match first, then the nearest entry
within `MATCH_TOLERANCE_PERMILLE = 120` (12 % of the larger side, per dimension, worst axis wins).

The tolerance is not decoration. The two readings of the *same* screen come from different APIs:
the settings screen reads the full panel (`Display.getRealMetrics()`), the connect path reads
`displayMetrics` (the window, system bars removed). In practice they sit 3-4 % apart, while the gap
between a foldable's halves is ~46 %. A tolerance is the only way one stored entry can serve both
readings without either screen borrowing the other's value.

`put(entries, w, h, dpi)` replaces an entry in place (order preserved, so saving does not shuffle
the list), appends a screen it has not seen, and *removes* the entry when `dpi <= 0` — so
"Automatic" means "no entry for this screen", never a stored zero.

## Where it is read

- `HeadUnitScreenConfig.densityDecision()` → `storedDensity()` → `Settings.dpiForScreen(panel)`,
  with the panel being `physicalWidthPx/HeightPx` and the canvas as the fallback when the display
  is unknown. Everything downstream is untouched: `AnnouncedDensityPolicy.decide()` still applies
  the HUIG 748x450 dp floor, the portrait 730 dp width floor and the "Use anyway" override. The
  per-screen value only replaces the *requested* number.
- `HeadUnitScreenConfig.computeSettingsHash()` carries the density **this screen gets**, not the
  shared one. Without that line, a value saved for one screen leaves a surface cached and measured
  under the other. It is the one non-obvious edit in the change; a reviewer should look at it first.
- `SettingsFragment.dpiRowValue()` reports the value for the panel it is being viewed on.
- `DpiSettingsFragment` shows `This screen: W × H px` and whether that size has a value of its
  own, edits *that* value, and saves through `storeDpiForScreen`. Automatic + Save deletes it.
- `OnboardingActivity.applyDisplaySettings()` stores per screen too, so running the wizard again
  does not overwrite the other screen's value.
- `SettingsBackupManager`: the entry is added to `backupKeys` (string) and to
  `projectionRestartKeys`, so export/import carries it and importing prompts a restart.

## Behaviour contract

| Situation | Result |
| --- | --- |
| the screen has an entry | that DPI, then the existing floors/clamp |
| the screen has no entry | shared `dpiPixelDensity` (0 = Automatic) — 1:1 the old behaviour |
| single-screen device | always matches its own entry; no visible change |
| Automatic chosen on a screen | that screen's entry is deleted |
| device rotated | same entry, the key is normalised |
| multi-window / split screen | the window reading may miss the tolerance and the shared value is used — a safe fallback, not a silent wrong pick |

Timing: the density is part of the service discovery video config, so it applies **at connect**. A
running session only accepts the UI config message, which carries margins (and the canvas), not the
density. Folding or unfolding mid-session therefore cannot switch the value; the session has to
reconnect. That is a protocol limit, not a gap in this change — and the accepted resolution and
canvas are fixed for exactly the same reason.

## Verification

- No JDK or Android SDK platform was available on the machine this was written on, so the build and
  the tests ran through GitHub Actions.
- `Android CI` on the fork: `Build (github debug)` and `Unit tests (github debug)` both green on the
  final commit.
- New `DensityScreenProfileTest`, 12 tests: parse/format round trip, unreadable entries dropped
  without costing the rest, orientation invariance, exact match, tolerant match across the system
  bars, fold halves never matching each other, replace-in-place, append on an unseen screen,
  Automatic removing an entry, unknown screen ignored.
- The debug APK was pulled from the workflow artifacts and checked to contain the final strings —
  a stale artifact is easy to hand out otherwise.
- The `Code Review` workflow fails on the fork with `GEMINI_API_KEY environment variable is
  required`. Unrelated: fork pull requests get no secrets. Android CI itself is fork-safe and green.

## Not done, deliberately

1. **Mid-session fold.** Nothing re-announces the density, and the resolution lock keeps the old
   negotiated resolution as well. A panel change would have to drop the lock and restart the
   session. Separate and riskier; worth its own change.
2. **Resolution is still one setting for both screens.** It is capped per panel, but not stored per
   screen — the same asymmetry this change fixed for the density.
3. **Automatic is still approximate on a foldable.** It derives from the global
   `displaySizePreset` diagonal, which is one number for two screens. A per-screen preset is the
   follow-up if people rely on Automatic instead of a set value.
4. **Translations.** Only `values/strings.xml` got the three new strings; the other locales fall
   back to English.
5. **No UI for listing or removing the other screens' entries.** To clear one, open the DPI screen
   on that screen and pick Automatic. The screen was kept to two caption lines on purpose.

## Open decisions before upstream

- **Where the option lives.** It is in the DPI screen. "Device-Specific Settings" was considered and
  rejected: that screen is a list of head-unit models (Adayo/Haval MCU hacks) filtered by a HU model
  dropdown, so a phone-side screen option does not fit there.
- **What Automatic means.** Today it is per screen ("no entry for this one"). "No entry anywhere"
  would be the other defensible reading; it would surprise less in some flows and more in others.
- **Whether the stored screens ever need a visible list.** Only if someone ends up with three or
  more screen sizes.

## Files touched

- `utils/DensityScreenProfile.kt` (new) — the whole policy, pure Kotlin, no Android imports.
- `utils/Settings.kt` — the pref, `dpiForScreen()`, `storeDpiForScreen()`.
- `utils/HeadUnitScreenConfig.kt` — `densityPanel()`, `storedDensity()`, the hash.
- `utils/SettingsBackupManager.kt` — backup key and restart key.
- `main/DpiSettingsFragment.kt`, `res/layout/fragment_dpi_settings.xml` — the per-screen line, the
  save path, the note under it.
- `main/SettingsFragment.kt` — the row value.
- `main/OnboardingActivity.kt` — the wizard's write.
- `res/values/strings.xml` — three strings.
- `test/.../utils/DensityScreenProfileTest.kt` (new) — 12 tests.

## Repo state at handoff

- Branch `graphic/per-screen-dpi` on the fork: one feature commit plus a wording fix.
- A pull request is open **in the fork** (against the fork's `main`, which was fast-forwarded to
  upstream `main`) purely so CI runs; it is not the upstream PR.
- Nothing has been written to the upstream repository: no branch, no PR, no comment.
