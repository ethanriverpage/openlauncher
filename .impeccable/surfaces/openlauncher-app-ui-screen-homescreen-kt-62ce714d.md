---
version: 1
slug: "openlauncher-app-ui-screen-homescreen-kt-62ce714d"
primary_target: "app/src/main/java/com/openlauncher/app/ui/screen/HomeScreen.kt"
related_targets: ["app/src/main/java/com/openlauncher/app/ui/components/Sidebar.kt","app/src/main/java/com/openlauncher/app/ui/screen/SettingsScreen.kt","app/src/main/java/com/openlauncher/app/ui/screen/ClimateScreen.kt","app/src/main/java/com/openlauncher/app/ui/screen/AppLibraryScreen.kt"]
---

# Launcher shell (home, apps, climate, settings)

Scope: the whole launcher shell, every screen, day and night. Mode: Operate.
Audience/job: drivers glancing at a wide dash screen; parked setup of shortcuts and widget layout.
Constraints: the user-arranged 3x2 drag/resize widget grid survives. Must not be bright or glowy at night, must not be text-heavy. Customization trimmed to accent tint, text size, display scale, and automatic day/night. A web mockup is refined before any Compose work (design/mockup/).
Resolved 2026-11-16: wallpaper is opt-in (off by default, dimmed, translucent tiles over it); vehicle name is a small label on the Clock widget; Left/Right/Bottom sidebar positions all stay; Weather offline is a quiet in-place tile.

## Direction contract

THESIS: Open Launcher as CarPlay-grade HIG on any head unit: a true-black dashboard of rounded system tiles that sits beside CarPlay without looking foreign. It refuses the incumbent console look (tracked mono caps, hairline boxes, labels over every value).
OWN-WORLD: iOS dark system palette: #000 ground, #1C1C1E tiles, #2C2C2E controls, labels at 100/60/30% white; light mode #F2F2F7 ground, white tiles. One user tint from the HIG system colors (default blue #0A84FF) marks live and selected state only. Inter stands in for SF, Material Symbols Rounded for SF Symbols. 28dp continuous corners, separation by elevation steps, no borders.
STORY: a glance gives time, track and heading at arm's-length sizes; one tap opens a sidebar app or tile; parked, a long-press enters iOS-style widget editing (remove badges, size menu, gallery sheet).
FIRST VIEWPORT: 104dp sidebar on the left: time and signal on top, app shortcuts as squircle icons, nav group (Home, Apps, Climate, Settings) at the bottom with the selected one filled. Right: 3x2 grid, 12dp gaps: Clock 1x1 in StandBy numerals, Weather 1x1, Compass 1x2, Now Playing 2x1 tinted from the artwork with a 72dp play button.
FORM: canon, the standing exit chosen by the user; craft bar Next-gen CarPlay plus iOS widgets/StandBy. seed: c08feab45efc295e. Signature move: Now Playing tinted from its artwork at low luminance, as CarPlay and StandBy do.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
