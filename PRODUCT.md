# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users
Drivers who have installed an aftermarket Android head unit in their car and replaced its stock launcher with this fork. Primary scene: seated in the driver's seat, glancing at a wide landscape touchscreen mounted in the dash, often while the car is moving, reaching with one arm. Secondary scene: parked, setting the launcher up (shortcuts, widget layout, assigning the CarPlay/Android Auto/radio apps).

The fork is maintained by its owner for their 2010 Lexus GX460 (Topway TS10 unit, 2400×896), but the redesign targets fork users generally: head units of different makes, resolutions and aspect ratios are equal targets.

## Product Purpose
An offline-first home screen for car head units. It gets the driver to the thing they need (CarPlay/Android Auto, music, radio, climate, camera, phone) with one glance and one tap, and shows the vehicle information worth glancing at (time, now playing, speed, compass, weather when online). Success: the driver never needs to study the screen, and the screen looks like it belongs in the car rather than like a phone app.

## Positioning
Free and open-source, works without a network connection, and talks to head-unit hardware directly where possible (szchoiceway MCU radio; Topway/TW climate via com.tw.ac and /dev/tw on this fork) instead of only launching vendor apps.

## Operating Context
- Head unit is offline most of the time; CarPlay or Android Auto provides navigation and media when a phone is connected.
- Wide landscape screens are the norm (roughly 1024×600 up to 2400×896 and beyond); low-to-mid power SoCs, so rendering cost matters.
- Bright daylight and dark night cabins; Day/Night switches by forced mode, system sync, or offline sunrise/sunset calculation.
- Physical HVAC buttons on supported units bring the climate screen forward.

## Capabilities and Constraints
- Screens: Home (3×2 widget grid with drag/resize and a widget library), App Library (also used as a picker for shortcuts/CarPlay/AA/PiP/radio apps), Settings, Climate (front + rear on GX460/TW units), first-run Onboarding, climate overlay activity.
- Navigation: a sidebar (left/right/bottom) with the destinations plus user-assigned app shortcuts (drag reorder, long-press remap).
- Widgets: Clock, Weather, Compass/Telemetry, Now Playing (with CarPlay/AA shortcut mode), Radio, Speedometer, Altimeter, Trip Meter, Vitals, Soundboard.
- Stack: Kotlin, Jetpack Compose (Material3 library as the component base), minSdk 21. No local Android SDK on the maintainer's Mac; builds run in GitHub Actions.
- Customization scope (confirmed for this redesign): keep accent tint, text size, UI scale, and automatic day/night; drop the font picker and other styling knobs that fight a consistent system look. Wallpaper stays as an opt-in (off by default, dimmed, extra dimming at night); custom background colors and gradients go.
- Layout decisions (2026-11-16): vehicle name shows as a small label on the Clock widget; sidebar keeps Left, Right and Bottom positions; Weather offline shows a quiet in-place tile rather than hiding and leaving a hole in the grid.

## Brand Commitments
- Design language: Apple's Human Interface Guidelines are binding for this redesign (user requirement), applied on Android hardware. The closest real-world reference for a car screen in that language is CarPlay.
- Name: Open Launcher. "OEM+" aesthetic: should look like it belongs in the car's interior.
- Apple's SF fonts are licensed only for Apple platforms and cannot be bundled in an Android APK.
- Standing preference (chosen 2026-11-16 on the direction round): the category standard played straight, meaning CarPlay-grade HIG with no house quirk. Craft bar: Next-gen CarPlay and iOS widgets/StandBy.
- Process: every redesign is refined as a web mockup before it is implemented in Compose.

## Evidence on Hand
- Screenshots of the current UI: `Screenshots /home.png`, `Screenshots /settings.png`, `Screenshots /applibrary.png`; README screenshots at 2400×896.
- Logo: `Screenshots /logoo.jpg`.
- No user testimonials or usage data.

## Product Principles
1. Glanceable first: anything shown while driving must read in under a second at arm's length.
2. One tap to the destination; setup and editing live behind deliberate gestures or Settings.
3. Works offline, degrades silently: missing data hides cleanly rather than showing broken states.
4. Belongs in the car: restrained, consistent, native-feeling; no gimmicks on the driving surface.
5. Adapts to any head unit: layouts hold across resolutions, aspect ratios and sidebar positions.

## Accessibility & Inclusion
Large touch targets for in-motion use (HIG CarPlay guidance is larger than phone minimums), legible contrast in both day and night, honour system font scale and the in-app text size setting.
