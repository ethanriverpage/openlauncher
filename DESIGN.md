---
name: Open Launcher
description: CarPlay-grade Apple HIG home screen for Android car head units, true black by night and iOS grouped grey by day.
colors:
  tint-blue-night: "#0A84FF"
  tint-blue-day: "#007AFF"
  night-ground: "#000000"
  night-tile: "#1C1C1E"
  night-tile-over-wallpaper: "#1C1C1EB8"
  night-fill: "#2C2C2E"
  night-fill-over-wallpaper: "#FFFFFF29"
  night-fill-2: "#3A3A3C"
  night-segment-thumb: "#636366"
  night-separator: "#54545899"
  night-label: "#FFFFFF"
  night-label-2: "#EBEBF59E"
  night-label-3: "#EBEBF552"
  night-menu: "#2C2C2EF5"
  night-sheet: "#1C1C1E"
  night-scrim: "#0000008C"
  day-ground: "#F2F2F7"
  day-tile: "#FFFFFF"
  day-tile-over-wallpaper: "#FFFFFFC7"
  day-fill: "#E5E5EA"
  day-fill-over-wallpaper: "#00000014"
  day-fill-2: "#D1D1D6"
  day-separator: "#3C3C4338"
  day-label: "#000000"
  day-label-2: "#3C3C439E"
  day-label-3: "#3C3C434D"
  day-menu: "#FAFAFCF7"
  day-sheet: "#F2F2F7"
  day-scrim: "#00000040"
  system-red-night: "#FF453A"
  system-red-day: "#FF3B30"
  system-green-night: "#30D158"
  system-green-day: "#34C759"
  system-orange-night: "#FF9F0A"
  system-orange-day: "#FF9500"
  system-yellow-night: "#FFD60A"
  system-yellow-day: "#FFCC00"
typography:
  large-title:
    fontFamily: "Inter Display (Inter variable, opsz 32), stand-in for SF Pro Display"
    fontSize: "34sp"
    fontWeight: 700
    letterSpacing: "-0.02em"
  title1:
    fontFamily: "Inter Display (Inter variable, opsz 32)"
    fontSize: "28sp"
    fontWeight: 700
    letterSpacing: "-0.015em"
  title2:
    fontFamily: "Inter (Inter variable, opsz 14), stand-in for SF Pro Text"
    fontSize: "24sp"
    fontWeight: 600
    letterSpacing: "-0.01em"
  headline:
    fontFamily: "Inter"
    fontSize: "19sp"
    fontWeight: 600
  body:
    fontFamily: "Inter"
    fontSize: "19sp"
    fontWeight: 400
  callout:
    fontFamily: "Inter"
    fontSize: "17sp"
    fontWeight: 400
  subhead:
    fontFamily: "Inter"
    fontSize: "16sp"
    fontWeight: 400
  footnote:
    fontFamily: "Inter"
    fontSize: "14sp"
    fontWeight: 400
    fontFeature: "tnum"
  caption:
    fontFamily: "Inter"
    fontSize: "13sp"
    fontWeight: 500
    fontFeature: "tnum"
  numeral:
    fontFamily: "Inter Display (Inter variable, opsz 32)"
    fontSize: "fitted to the tile by the caller (clock: min(46% tile height, 30% tile width))"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "-0.035em"
    fontFeature: "tnum"
rounded:
  segment-thumb: "9dp"
  segment-track: "12dp"
  artwork: "14dp"
  control: "16dp"
  group: "16dp"
  menu: "16dp"
  stepper: "20dp"
  alert: "20dp"
  toggle-tile: "22dp"
  sidebar-group: "22dp"
  widget-gallery-card: "22dp"
  tile: "28dp"
  sheet: "28dp"
  squircle-icon: "23.5% of icon size"
  capsule: "50%"
spacing:
  hairline: "0.5dp"
  xs: "6dp"
  sm: "8dp"
  grid-gap: "12dp"
  md: "16dp"
  row-inset: "18dp"
  tile-padding: "24dp"
  section-gap: "30dp"
  sidebar-width: "104dp"
  sidebar-width-tight: "88dp"
components:
  widget-tile:
    backgroundColor: "{colors.night-tile}"
    rounded: "{rounded.tile}"
    padding: "24dp"
  widget-tile-day:
    backgroundColor: "{colors.day-tile}"
    rounded: "{rounded.tile}"
    padding: "24dp"
  pill-button-primary:
    backgroundColor: "{colors.tint-blue-night}"
    textColor: "{colors.night-label}"
    typography: "{typography.headline}"
    rounded: "{rounded.capsule}"
    height: "52dp"
    padding: "0 22dp"
  pill-button-secondary:
    backgroundColor: "{colors.night-fill}"
    textColor: "{colors.night-label}"
    typography: "{typography.headline}"
    rounded: "{rounded.capsule}"
    height: "52dp"
    padding: "0 22dp"
  step-button:
    backgroundColor: "{colors.night-fill}"
    textColor: "{colors.night-label}"
    rounded: "{rounded.stepper}"
    width: "76dp"
    height: "64dp"
  circle-icon-button:
    textColor: "{colors.night-label}"
    rounded: "{rounded.capsule}"
    size: "64dp"
  now-playing-play-button:
    backgroundColor: "#FFFFFF24"
    textColor: "{colors.night-label}"
    rounded: "{rounded.capsule}"
    size: "76dp"
  toggle-tile-off:
    backgroundColor: "{colors.night-tile}"
    textColor: "{colors.night-label-2}"
    rounded: "{rounded.toggle-tile}"
    height: "96dp"
  toggle-tile-on-day:
    backgroundColor: "{colors.tint-blue-day}"
    textColor: "{colors.day-tile}"
    rounded: "{rounded.toggle-tile}"
  segmented-control:
    backgroundColor: "{colors.night-fill}"
    textColor: "{colors.night-label-2}"
    typography: "{typography.subhead}"
    rounded: "{rounded.segment-track}"
    padding: "3dp"
  segmented-control-selected:
    backgroundColor: "{colors.night-segment-thumb}"
    textColor: "{colors.night-label}"
    rounded: "{rounded.segment-thumb}"
    height: "44dp"
  switch-on:
    backgroundColor: "{colors.system-green-night}"
    rounded: "{rounded.capsule}"
    width: "64dp"
    height: "38dp"
  switch-off:
    backgroundColor: "{colors.night-fill-2}"
    rounded: "{rounded.capsule}"
    width: "64dp"
    height: "38dp"
  grouped-section:
    backgroundColor: "{colors.night-tile}"
    rounded: "{rounded.group}"
  list-row:
    textColor: "{colors.night-label}"
    typography: "{typography.body}"
    height: "64dp"
    padding: "10dp 18dp"
  context-menu:
    backgroundColor: "{colors.night-menu}"
    textColor: "{colors.night-label}"
    typography: "{typography.body}"
    rounded: "{rounded.menu}"
    width: "300dp"
  alert:
    backgroundColor: "{colors.night-menu}"
    textColor: "{colors.night-label}"
    rounded: "{rounded.alert}"
    width: "380dp"
  sidebar-nav-group:
    backgroundColor: "{colors.night-tile}"
    rounded: "{rounded.sidebar-group}"
    padding: "6dp"
  sidebar-nav-item-active:
    backgroundColor: "{colors.night-fill}"
    textColor: "{colors.night-label}"
    rounded: "{rounded.control}"
    width: "64dp"
    height: "56dp"
  sidebar-nav-item:
    textColor: "{colors.night-label-2}"
    rounded: "{rounded.control}"
    width: "64dp"
    height: "56dp"
  widget-caption:
    textColor: "{colors.night-label-2}"
    typography: "{typography.footnote}"
---

# Design System: Open Launcher

## 1. Overview

**Creative North Star: "The CarPlay Dashboard"**

Open Launcher is Apple's Human Interface Guidelines played straight on Android head units: a true-black dashboard of rounded system tiles that could sit beside CarPlay without looking foreign. There is no house quirk. The craft bar is next-gen CarPlay and iOS widgets/StandBy, and every decision is measured against "would this look native next to CarPlay at arm's length, at night, while the car is moving?" Inter stands in for SF Pro (SF is licensed only for Apple platforms) and Material Symbols Rounded stand in for SF Symbols.

The surface is low-density and glance-first. A 104dp sidebar holds status, squircle app shortcuts and a grouped nav capsule; the rest of the screen is a user-arranged 3x2 grid of 28dp tiles separated by 12dp gaps. Each tile carries one big StandBy-style numeral or one control cluster, a small caption top-left, and nothing else. Depth comes from stepping up the iOS elevation ladder (ground, tile, fill), never from outlines. One user tint, picked from the HIG system colours, marks live and selected state only.

Night calm is a core requirement, not a theme. Night is pure #000 ground with #1C1C1E tiles, and anything that would become a bright field in a dark cabin is dimmed by rule: active toggles become a 30% tint wash, wallpaper gains an extra 15% dim. The system explicitly rejects the incumbent console look (tracked mono caps, hairline boxes, labels over every value), phone-app chrome on the driving surface, and anything bright or glowy at night.

The target head unit is 1920x720 at 240dpi, so the working canvas is 1280x480dp; every layout must hold there. The sidebar shrinks rather than scrolls on short screens: under 660dp tall is compact (54dp icons, 48dp nav buttons, 10dp gaps), under 540dp tall is tight (46dp icons, 42dp nav buttons, 8dp gaps, 88dp wide). Left, right and bottom sidebar positions are all first-class.

**Key Characteristics:**
- True-black night ground, iOS grouped grey (#F2F2F7) day ground, both from the iOS semantic palette resolved per appearance.
- One tint, live state only; ten HIG presets, each with its own day and night variant.
- Separation by elevation step (ground, tile, fill, fill-2), never by borders.
- Continuous large radii: 28dp tiles and sheets, 22dp toggle tiles, 16dp controls and groups, capsules for buttons.
- StandBy numerals: Inter Display at optical size 32, semibold, tabular, -0.035em, sized to fill the tile.
- Touch targets sized for in-motion use: 52dp pills, 64dp list rows, 64-76dp steppers and transport buttons.
- Gentle iOS press dip (scale 0.94, spring stiffness 900) on every tappable control instead of ripples.

## 2. Colors

The iOS dark and light system palettes, used literally, with a single user-chosen tint and the four HIG status colours.

### Primary
- **System Blue (default tint)** (#0A84FF night / #007AFF day): the only accent. Fills the primary pill button (CarPlay launch), the slider's active track, lit fan-level bars, alert action text, selection rings in pickers, and the "on" state of toggle tiles by day. Users may swap it for any HIG preset: Indigo (#5E5CE6 / #5856D6), Purple (#BF5AF2 / #AF52DE), Pink (#FF375F / #FF2D55), Red (#FF453A / #FF3B30), Orange (#FF9F0A / #FF9500), Yellow (#FFD60A / #FFCC00), Green (#30D158 / #34C759), Teal (#40C8E0 / #30B0C7), Graphite (#8E8E93). Text on the tint flips to black when the tint's luminance exceeds 0.55.

### Neutral
- **Night Ground** (#000000): the night canvas behind everything. Pure black so the panel's backlight is the only glow.
- **Night Tile** (#1C1C1E, 72% opaque over wallpaper): every widget tile, grouped list section, nav capsule, sheet.
- **Night Fill** (#2C2C2E, white at 16% over wallpaper): controls sitting on a tile: steppers, secondary pills, segmented track, the active nav item.
- **Night Fill 2** (#3A3A3C): one step further: switch track off, slider rail, empty-slot outlines, sheet grabber.
- **Night Labels** (#FFFFFF / #EBEBF5 at 62% / #EBEBF5 at 32%): primary, secondary (captions, subtitles, inactive nav glyphs), tertiary (disabled, chevrons, offline glyph).
- **Night Separator** (#545458 at 60%): 0.5dp hairlines inside grouped lists, menus and alerts only.
- **Night Menu** (#2C2C2E at 96%): context menus and alerts.
- **Day Ground** (#F2F2F7), **Day Tile** (#FFFFFF, 78% over wallpaper), **Day Fill** (#E5E5EA, black at 8% over wallpaper), **Day Fill 2** (#D1D1D6), **Day Labels** (#000000 / #3C3C43 at 62% / #3C3C43 at 30%), **Day Separator** (#3C3C43 at 22%), **Day Menu** (#FAFAFC at 97%): the same roles resolved for daylight.
- **Scrims** (black at 55% night, 25% day): behind the widget gallery sheet and dialogs.

### Status
- **System Red / Green / Orange / Yellow** (night #FF453A / #30D158 / #FF9F0A / #FFD60A; day #FF3B30 / #34C759 / #FF9500 / #FFCC00): red for destructive menu actions and alert confirmations, green for the switch "on" track, orange and yellow for vitals warnings and weather glyphs. They are status colours, never decoration.

### Named Rules
**The One Tint Rule.** The tint marks live or selected state and nothing else. It is never a background wash for a whole tile, a heading colour, or decoration. White and black are not tints: a stored white or black accent (luminance above 0.85 or below 0.02) resolves to System Blue.

**The Night Dim Rule.** At night a selected toggle is never a solid tint. It is the tint at 30% composited over the tile, with its glyph and label lerped 45% from the tint toward white. By day the same toggle is the solid tint with on-tint text. A row of active buttons must never become a bright field in a dark cabin.

**The Translucent Over Wallpaper Rule.** Wallpaper is opt-in and off by default. When it is on, tiles and fills go translucent (night tile 72%, day tile 78%) and the wallpaper is dimmed by the user's setting (default 0.55, range 0.2 to 0.85), plus 0.15 more at night, capped at 0.92.

**The Artwork Tint Rule.** Now Playing is the one tile allowed a colour of its own: the artwork's average colour at 34% (night) or 26% (day) composited over the tile. Low luminance is the point; it reads as CarPlay, not as a light.

## 3. Typography

**Display Font:** Inter Display, the Inter variable font pinned to optical size 32 (stand-in for SF Pro Display)
**Body Font:** Inter at optical size 14 (stand-in for SF Pro Text)

**Character:** One family doing what SF does on iOS: a tight display cut for big numerals and titles, an open text cut for everything else. Sizes are iOS points scaled up for arm's-length reading on a dash, and every style multiplies by the user's Text Size setting.

### Hierarchy
- **Numeral** (600, fitted to the tile, line-height 1, -0.035em, tabular): clock, weather temperature, heading, climate temperatures, speed, altitude, radio frequency. Sized by the caller to fill its tile (the clock takes the smaller of 46% of tile height and 30% of tile width).
- **Large Title** (700, 34sp, -0.02em): screen titles in Settings and App Library.
- **Title 1** (700, 28sp, -0.015em): letter avatars for apps without icons; large section heads.
- **Title 2** (600, 24sp, -0.01em): track title, "Not Playing", sheet and dialog titles.
- **Headline** (600, 19sp): pill button labels, sidebar time, alert titles and cancel action.
- **Body** (400, 19sp): list row titles, artist line, menu items, row values.
- **Callout** (400, 17sp): secondary descriptive text.
- **Subhead** (400, 16sp): row subtitles, alert messages, segmented labels (at 600).
- **Footnote** (400, 14sp, tabular): widget captions (at 600), grouped section headers and footers.
- **Caption** (500, 13sp, tabular): source app name, elapsed and remaining time.

Stock Material3 pieces still in use (text fields, menus) are re-pointed to Inter on a matching 13-57sp scale, so nothing renders in Roboto.

### Named Rules
**The Numeral Carries The Tile Rule.** A glanceable tile shows one value at numeral size and a caption; it does not stack labels over values. If a tile needs a label above every number, it is the console look this system replaced.

**The Grouped Header Rule.** Uppercase appears in exactly one place: the iOS grouped-list section header in Settings (footnote, secondary label, 18dp inset). Nowhere on the driving surface.

## 4. Elevation

The system is flat and tonal at rest. Depth is the iOS elevation ladder (ground, tile, fill, fill-2), each step a lighter grey at night and a darker grey on white by day. Shadows exist only where iOS itself draws them: on things that physically lift (a dragged tile, a menu, artwork) and on white knobs and thumbs that sit on a track.

### Shadow Vocabulary
- **Menu lift** (24dp elevation, 16dp rounded): context menus floating over the grid.
- **Drag lift** (18dp elevation, tile shape): a widget tile while being dragged in edit mode.
- **Artwork** (12dp elevation, 14dp rounded): album art in Now Playing.
- **Edit badge** (6dp elevation, circle): the 44dp remove and resize badges on tiles in edit mode.
- **Knob** (4dp slider knob, 3dp switch knob, circle): white knobs on tracks.
- **Segment thumb** (2dp, 9dp rounded): the selected segment.

### Named Rules
**The Step Not Stroke Rule.** Surfaces separate by stepping up the ladder, never by a border. Outlines are allowed only for state that has no surface yet: an empty shortcut slot or "+" pad (2dp fill-2), a drop target in edit mode (2dp tint over a 16% tint wash), a selection ring in a picker (3dp tint). Hairline separators (0.5dp) live only inside grouped lists, menus and alerts, as on iOS.

**The Flat At Rest Rule.** A tile never carries a shadow while it sits in the grid. If tiles look like cards floating on a page, the shadow is wrong.

## 5. Components

Every interactive element dips to 94% scale under the finger (spring, stiffness 900) with no ripple. Transitions are short tweens (150-260ms fades and expands, 380ms sheet rise) and springs for knobs (damping 0.75, stiffness 700).

### Buttons
- **Shape:** capsules (50%) for text buttons; rounded rectangles (20dp) for steppers; circles for transport.
- **Pill Button:** 52dp tall, 22dp horizontal padding, optional 24dp leading glyph, headline label. Primary is solid tint with on-tint text; secondary sits on fill with label text. Disabled text drops to label-3.
- **Step Button:** 76x64dp, 20dp rounded, fill background, 38dp glyph. Climate temperature arrows and fan minus/plus.
- **Circle Icon Button:** 64dp with a 40dp glyph, transparent by default. The Now Playing play/pause is 76dp with a 46dp glyph on label at 14%.

### Toggle Tiles
- **Style:** 22dp rounded, 96dp tall in the climate row, glyph (32dp) over a label, tile background when off.
- **State:** on follows The Night Dim Rule (solid tint by day, 30% tint wash with lightened glyph at night). Radio presets and soundboard pads use the same toggle-on colour.

### Controls
- **Segmented:** fill track, 12dp rounded, 3dp inset; selected thumb is #636366 at night or white by day, 9dp rounded, 2dp shadow, 44dp minimum height, subhead at 600.
- **Switch:** 64x38dp capsule, System Green when on, fill-2 when off, white 32dp knob with 3dp shadow.
- **Slider:** 44dp touch height, 6dp capsule rail in fill-2, tint fill for the active span, white 30dp knob with 4dp shadow.
- **Fan level:** seven bars rising from 30% to 100% height, 6dp rounded, 6dp gaps; lit bars in tint, unlit in fill.

### Cards / Containers
- **Widget Tile:** 28dp rounded, tile colour, 24dp internal padding, 12dp gaps in the 3x2 grid with 12dp outer margin. Caption top-left (18dp glyph plus footnote at 600 in label-2), one numeral or control cluster, nothing else.
- **Grouped Section:** 16dp rounded tile-colour group, rows divided by 0.5dp separators inset 18dp, optional uppercase footnote header and sentence-case footnote footer, 30dp below each section.
- **Sheet:** widget gallery rises from the bottom with 28dp top corners, a 44x5dp grabber in fill-2, adaptive 230dp cards (22dp rounded, 18dp padding, 34dp tint glyph).

### Lists
- **List Row:** 64dp minimum height, 18dp horizontal and 10dp vertical padding, 16dp gap, body title (two lines max) with subhead subtitle in label-2.
- **Disclosure Row:** value in label-2 then a 28dp chevron in label-3.
- **Squircle Glyph:** 34dp coloured rounded square (27% radius) with a white 22dp glyph, as in iOS Settings.

### Menus and Alerts
- **Context Menu:** 300dp wide, menu colour, 16dp rounded, 24dp shadow, 56dp rows with 18dp padding; a 28dp check column on the left, trailing 24dp glyph; destructive actions in red, separated from the rest by an 8dp black-at-18% group gap.
- **Alert:** 380dp wide, 20dp rounded, menu colour, centred headline and subhead message, two 60dp buttons split by hairlines (Cancel in headline tint, confirm in body tint or red).

### Navigation
- **Sidebar:** 104dp wide (88dp tight). Top: time in headline with a 16dp connectivity glyph (Wi-Fi or cellular in label-2, offline in label-3). Middle: app shortcuts as squircle icons (64 / 54 / 46dp by tier, 23.5% radius), long-press for a context menu, drag to reorder. Bottom: a 22dp-rounded tile-colour capsule holding Home, Apps, Climate, Settings as 64dp-wide items; the active one gets a 16dp-rounded fill and its filled glyph in label, the rest stay outlined in label-2. Along the bottom edge the same parts lay out in a 92dp row.
- **Default shortcut glyphs:** white glyph on a two-stop HIG gradient (Radio pink #FF375F to #D70F3C, Phone green #34C759 to #16A34A, and so on), standing in for real app icons when none is assigned.

### Now Playing (signature)
A 2x1 tile tinted from its artwork per The Artwork Tint Rule. Wide layouts show 78%-height artwork (14dp rounded, 12dp shadow), then source app (caption), title (title 2), artist (body), a 6dp progress capsule (label at 18% rail, label fill) with elapsed and remaining in caption, then previous / 76dp play / next. Narrow tiles stack metadata over centred controls. With nothing playing it offers the CarPlay and Android Auto pills instead of an empty state.

### Climate
Driver and passenger tiles each carry a fitted numeral temperature (dimmed to label-3 when the system is off) over two step buttons; the centre tile holds the fan level and airflow mode; a 96dp row of toggle tiles (Off, Auto, A/C, Recirculate, Dual, Rear Defrost, Rear Seats) runs along the bottom. The overlay activity reuses the same content in a 32dp-rounded floating panel.

## 6. Do's and Don'ts

### Do:
- **Do** resolve every colour through `Hig.colors` for the current appearance and wallpaper state; never hard-code a hex in a screen.
- **Do** keep the tint for live and selected state only (The One Tint Rule).
- **Do** apply the night toggle-on rule (tint at 30% over tile, label lerped 45% toward white) to every selectable control.
- **Do** separate surfaces by stepping ground, tile, fill, fill-2, with 28dp tiles and 12dp gaps.
- **Do** let one fitted numeral carry a glanceable tile, with a single caption top-left.
- **Do** size touch targets for a moving car: 52dp pills, 64dp rows, 64-76dp steppers and transport buttons, 44dp minimum anywhere.
- **Do** test every layout at 1280x480dp (the 1920x720 @ 240dpi head unit) in the tight sidebar tier, and with the sidebar on the left, right and bottom.
- **Do** refine any new surface as a web mockup (design/mockup/) before writing Compose.
- **Do** degrade quietly: an offline Weather tile stays in place as a quiet tile rather than leaving a hole.

### Don't:
- **Don't** bring back the incumbent console look: "tracked mono caps, hairline boxes, labels over every value".
- **Don't** make anything "bright or glowy at night": no solid-tint toggle rows, no saturated tile backgrounds, no glows.
- **Don't** draw borders to separate surfaces; outlines are reserved for empty slots, drop targets and selection rings.
- **Don't** put uppercase eyebrows or kickers on tiles, captions or headings; uppercase belongs only to grouped-list section headers.
- **Don't** use white or black as the accent; they resolve to System Blue.
- **Don't** reintroduce the font picker, custom background colours or gradients; customization is tint, text size, display scale, automatic day/night and opt-in wallpaper.
- **Don't** add gimmicks to the driving surface or make it text-heavy; it must "look like it belongs in the car rather than like a phone app".
- **Don't** use Material ripples, elevated Material cards or Roboto; press feedback is the iOS dip and every style is on Inter.
- **Don't** let the sidebar scroll on short screens; it shrinks to the compact or tight tier.
