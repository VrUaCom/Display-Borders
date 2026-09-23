# Display Borders — Рамки дисплею

**Display Borders** is an Android utility that reduces the logical working area
of the physical display. The black border is not a fake overlay: Android is
instructed to use a smaller logical viewport, so apps and system UI are laid out
inside the new bounds instead of rendering behind the border.

> Project status: early Android prototype / source release.

## Core behaviour

- One mode only: **black border = outside the working display**.
- Symmetric left/right border and symmetric top/bottom border.
- Android receives a smaller logical resolution using `wm size WxH`.
- Automatic scaling is disabled with `wm scaling off`, preventing the smaller
  logical viewport from being stretched back over the full panel.
- Reset restores `wm size reset` and `wm scaling auto`.
- The setting remains active after Display Borders or Shizuku is closed, until
  it is reset or changed.

## Why Shizuku

Changing WindowManager's forced display size is a shell/system operation. A
normal Android application cannot do this using ordinary app permissions.
Display Borders uses a Shizuku UserService so the command can execute with ADB
shell identity (or root/Sui where applicable).

On Android 11+ Shizuku can be started through Wireless debugging without a PC.

## Build

Open the project in Android Studio and build the `app` module.

Current dependency: Shizuku API **13.1.5**.

The current Java/Kotlin package namespace still uses the original internal
prototype namespace `com.viktor.edgeguard`; the public product name is
**Display Borders**.

## Usage

1. Install and start Shizuku.
2. Open **Display Borders**.
3. Grant the requested Shizuku permission.
4. Set the horizontal and vertical border sizes in pixels.
5. Tap **Застосувати рамку**.
6. Use **Повернути весь екран** to restore the factory viewport.

## Safety / recovery

The UI prevents a viewport smaller than 320 px on either axis. If the UI becomes
unusable, reset the forced display configuration from an ADB shell:

```sh
wm size reset
wm scaling auto
```

## Official app distribution

The official compiled **Display Borders** application is distributed **free of
charge through Google Play** by the copyright holder.

Free end-user distribution of the official app does **not** place the source
code in the public domain, does not make the project open source, and does not
grant third parties the right to commercially redistribute, rebrand, sell, or
license Display Borders.

## License


**This is not an open-source license and commercial use is not included in the
free license.**

Copyright © 2026 Victor (GitHub: **VrUaCom**). All rights reserved.

- Personal, educational, research, testing, and other non-commercial use is
  permitted under [`LICENSE`](LICENSE).
- Commercial use requires a separate paid Commercial Integration License.
- Standard commercial license price: **USD 299 per licensee** (individual or
  legal entity), unless otherwise agreed in writing.
- The commercial license permits the licensee to integrate Display Borders code
  or functionality into the licensee's own products as part of their
  functionality.
- It does **not** permit resale, relicensing, sublicensing, or redistribution of
  Display Borders itself as a standalone/separable product, SDK, library,
  toolkit, or codebase.

See [`COMMERCIAL-LICENSE.md`](COMMERCIAL-LICENSE.md) for the commercial terms.

For commercial licensing, contact **VrUaCom** through GitHub.
