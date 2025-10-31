# Responsive Toolbar - Visual Guide

**Task:** 37943-toolbar-search-filter  
**Subtask:** 4 - Responsive Layout

---

## Breakpoint Overview

```
Mobile     Tablet        Desktop
├──────────┼─────────────┼──────────────>
0px       768px        1280px
(base)     (md)          (xl)

Vertical   Semi-Vert     Horizontal
Layout     + Rows        Layout
```

---

## Visual Representations

### 1. Mobile View (< 768px)

#### Collapsed State

```
┌────────────────────────────────┐
│                                │
│  ┌──────────────────────────┐ │
│  │                          │ │
│  │       [▼] 🔍            │ │
│  │                          │ │
│  └──────────────────────────┘ │
│                                │
│        Workspace Canvas        │
│                                │
└────────────────────────────────┘
```

#### Expanded State

```
┌────────────────────────────────┐
│ ┌────────────────────────────┐ │
│ │ ╔════════════════════════╗ │ │
│ │ ║ SEARCH & FILTER        ║ │ │
│ │ ╠════════════════════════╣ │ │
│ │ ║                        ║ │ │
│ │ ║ ┌────────────────────┐ ║ │ │
│ │ ║ │ Search Input       │ ║ │ │
│ │ ║ └────────────────────┘ ║ │ │
│ │ ║                        ║ │ │
│ │ ║ ┌────────────────────┐ ║ │ │
│ │ ║ │ Filter Button      │ ║ │ │
│ │ ║ └────────────────────┘ ║ │ │
│ │ ║                        ║ │ │
│ │ ╚════════════════════════╝ │ │
│ │ ────────────────────────── │ │
│ │ ╔════════════════════════╗ │ │
│ │ ║ LAYOUT CONTROLS        ║ │ │
│ │ ╠════════════════════════╣ │ │
│ │ ║                        ║ │ │
│ │ ║ ┌────────────────────┐ ║ │ │
│ │ ║ │ Algorithm Selector │ ║ │ │
│ │ ║ └────────────────────┘ ║ │ │
│ │ ║                        ║ │ │
│ │ ║ ┌────────────────────┐ ║ │ │
│ │ ║ │ Apply Layout       │ ║ │ │
│ │ ║ └────────────────────┘ ║ │ │
│ │ ║                        ║ │ │
│ │ ║ ┌─────────┬──────────┐ ║ │ │
│ │ ║ │ Presets │ Settings │ ║ │ │
│ │ ║ └─────────┴──────────┘ ║ │ │
│ │ ║                        ║ │ │
│ │ ╚════════════════════════╝ │ │
│ │                            │ │
│ │      [▲ Collapse]          │ │
│ └────────────────────────────┘ │
│                                │
│        Workspace Canvas        │
│                                │
└────────────────────────────────┘
```

**Key Features:**

- Full viewport width (100vw)
- Vertical stacking
- All buttons full width
- Down ▼ / Up ▲ arrows
- Larger touch targets (48px)
- Gap: 3 (12px)
- Padding: 3 (12px)

---

### 2. Tablet View (768px - 1279px)

#### Collapsed State

```
┌────────────────────────────────────────┐
│                                        │
│  ┌──────────────────────────┐          │
│  │       [▼] 🔍            │          │
│  └──────────────────────────┘          │
│                                        │
│           Workspace Canvas             │
│                                        │
└────────────────────────────────────────┘
```

#### Expanded State

```
┌────────────────────────────────────────┐
│ ┌────────────────────────────────────┐ │
│ │ ╔══════════════════════════════╗   │ │
│ │ ║ SEARCH & FILTER              ║   │ │
│ │ ╠══════════════════════════════╣   │ │
│ │ ║ ┌──────────┐  ┌────────────┐ ║   │ │
│ │ ║ │  Search  │  │   Filter   │ ║   │ │
│ │ ��� └──────────┘  └────────────┘ ║   │ │
│ │ ╚══════════════════════════════╝   │ │
│ │ ──────────────────────────────────  │ │
│ │ ╔══════════════════════════════╗   │ │
│ │ ║ LAYOUT CONTROLS              ║   │ │
│ │ ╠══════════════════════════════╣   │ │
│ │ ║ ┌────────┐ ┌──────────────┐ ║   │ │
│ │ ║ │Selector│ │ Apply Layout │ ║   │ │
│ │ ║ └────────┘ └──────────────┘ ║   │ │
│ │ ║ ┌────────┐ ┌────────────┐   ║   │ │
│ │ ║ │Presets │ │  Settings  │   ║   │ │
│ │ ║ └────────┘ └────────────┘   ║   │ │
│ │ ╚══════════════════════════════╝   │ │
│ │                                    │ │
│ │           [▲ Collapse]             │ │
│ └────────────────────────────────────┘ │
│                                        │
│           Workspace Canvas             │
│                                        │
└────────────────────────────────────────┘
```

**Key Features:**

- 90% viewport width (90vw)
- Still vertical stacking
- Controls in rows within sections
- Auto-width for buttons (natural size)
- Down ▼ / Up ▲ arrows
- Gap: 3 (12px)
- Padding: 3 (12px)

---

### 3. Desktop View (>= 1280px)

#### Collapsed State

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  ┌────────┐                                                 │
│  │ [→] 🔍│                                                 │
│  └────────┘                                                 │
│                                                              │
│                   Workspace Canvas                           │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

#### Expanded State

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ [Search....] [Filter🎯] │ [Algo▼] [Apply] [⭐] [⚙️] [◀] │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│                   Workspace Canvas                           │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Detailed View:**

```
┌────────────────────────────────────────────────────────────────┐
│ ╔══════════════════════╦══════════════════════════════════════╗│
│ ║ SEARCH & FILTER      ║ LAYOUT CONTROLS                      ║│
│ ╠══════════════════════╬══════════════════════════════════════╣│
│ ║                      ║                                      ║│
│ ║ ┌─────────────────┐  ║ ┌──────────┐ ┌────────────────────┐║│
│ ║ │ Search Input... │  ║ │Algo ▼    │ │ Apply Layout ▶    │║│
│ ║ └─────────────────┘  ║ └──────────┘ └────────────────────┘║│
│ ║                      ║                                      ║│
│ ║ ┌───────────────┐    ║ ┌──────┐ ┌─────────┐              ║│
│ ║ │ Filter 🎯     │    ║ │ ⭐   │ │  ⚙️     │    [◀]       ║│
│ ║ └───────────────┘    ║ └──────┘ └─────────┘              ║│
│ ║                      ║                                      ║│
│ ╚══════════════════════╩══════════════════════════════════════╝│
└────────────────────────────────────────────────────────────────┘
```

**Key Features:**

- Max width: 1280px
- Horizontal layout
- Compact spacing
- Vertical divider │
- Right → / Left ◀ arrows
- Standard height (40px)
- Gap: 2 (8px)
- Padding: 2 (8px)

---

## Icon Transformations

### Expand Button

```
Mobile (base):          Desktop (xl):
┌──────────���           ┌──────────┐
│          │           │          │
│    ▼     │           │    →     │
│    🔍    │           │    🔍    │
│          │           │          │
└──────────┘           └──────────┘
   Down                  Right
 (expands               (expands
  downward)             rightward)
```

**CSS:**

```tsx
transform={{
  base: 'rotate(-90deg)',  // Chevron points down
  xl: 'rotate(0deg)'        // Chevron points right
}}
```

---

### Collapse Button

```
Mobile (base):          Desktop (xl):
┌──────────┐           ┌──────────┐
│          │           │          │
│    ▲     │           │    ◀     │
│          │           │          │
└──────────┘           └──────────┘
    Up                   Left
(collapses             (collapses
  upward)               leftward)
```

**CSS:**

```tsx
transform={{
  base: 'rotate(90deg)',   // Chevron points up
  xl: 'rotate(0deg)'        // Chevron points left
}}
```

---

## Divider Transformation

### Horizontal (Mobile/Tablet)

```
┌────────────────────────────┐
│  SEARCH & FILTER SECTION   │
└────────────────────────────┘
────────────────────────────── ← Horizontal divider
┌────────────────────────────┐
│   LAYOUT CONTROLS SECTION  │
└────────────────────────────┘
```

### Vertical (Desktop)

```
┌──────────────────┬──────────────────┐
│                  │                  │
│  SEARCH &        │  LAYOUT          │
│  FILTER          │  CONTROLS        │
│  SECTION         │  SECTION         │
│                  │                  │
└──────────────────┴──────────────────┘
                   ↑
            Vertical divider
```

**CSS:**

```tsx
orientation={dividerOrientation}  // 'horizontal' or 'vertical'
h={{ base: 'auto', xl: '24px' }}
```

---

## Tooltip Placement

### Mobile - Top Placement

```
       ┌──────────────┐
       │ Layout       │
       │ Options      │
       └───────┬──────┘
               │
          ┌────▼────┐
          │   ⚙️    │
          └─────────┘
```

**Why:** More screen space above on mobile

### Desktop - Bottom Placement

```
          ┌─────────┐
          │   ⚙️    │
          └────▲────┘
               │
       ┌───────┴──────┐
       │ Layout       │
       │ Options      │
       └──────────────┘
```

**Why:** Traditional desktop pattern

---

## Spacing Scale

### Mobile (base)

```
Gap:     12px (3 units)
Padding: 12px (3 units)

┌─────────────────────────┐
│░░░░░░░░░░░░░░░░░░░░░░░░░│ ← 12px padding
│░┌───────────────────┐░░░│
│░│ Button            │░░░│
│░└───────────────────┘░░░│
│░░░░░░░░░12px░░░░░░░░░░░░│ ← Gap between elements
│░┌───────────────────┐░░░│
│░│ Button            │░░░│
│░└───────────────────┘░░░│
│░░░░░░░░░░░░░░░░░░░░░░░░░│
└─────────────────────────┘
```

### Desktop (xl)

```
Gap:     8px (2 units)
Padding: 8px (2 units)

┌──────────────────────────┐
│░░░░░░░░░░░░░░░░░░░░░░░░░░│ ← 8px padding
│░[Btn] 8px [Btn] 8px [Btn]│
│░░░░░░░░░░░░░░░░░░░░░░░░░░│
└──────────────────────────┘
```

---

## Button Width Behavior

### Mobile (base) - Full Width

```
┌────────────────────────────┐
│                            │
│ ┌────────────────────────┐ │
│ │ Search Input           │ │ 100% width
│ └────────────────────────┘ │
│                            │
│ ┌────────────────────────┐ │
│ │ Filter Button          │ │ 100% width
│ └────────────────────────┘ │
│                            │
└────────────────────────────┘
```

### Tablet (md) - Auto Width

```
┌────────────────────────────────┐
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │  Search  │  │    Filter    │ │ auto width
│ └──────────┘  └──────────────┘ │
│                                │
└────────────────────────────────┘
```

### Desktop (xl) - Inline

```
┌──────────────────────────────────────┐
│ [Search] [Filter] │ [Algo] [Apply]   │ natural inline width
└──────────────────────────────────────┘
```

**CSS:**

```tsx
sx={{
  '& > *': {
    width: { base: '100%', md: 'auto' },
  },
}}
```

---

## Z-Index & Layering

All sizes maintain same layering:

```
Layer 3: Drawers (modal, settings)
Layer 2: Toolbar (expanded)
Layer 1: Toolbar (collapsed)
Layer 0: Canvas
```

---

## Animation Consistency

All breakpoints use same timing:

```
Transition: 0.4s cubic-bezier(0.4, 0, 0.2, 1)
Duration:   ANIMATION.TOOLBAR_ANIMATION_DURATION_MS (400ms)
```

**Smooth across:**

- Width changes
- Opacity fade
- Transform (rotation)
- Layout shifts

---

## Touch Target Sizes

### Mobile

- Minimum: 48px × 48px ✅
- Buttons: Full width (easy to tap)
- Icons: 24px × 24px in 48px container

### Desktop

- Minimum: 40px × 40px ✅
- Buttons: Natural size (compact)
- Icons: 24px × 24px in 40px container

---

## Responsive Testing Checklist

- [x] Test at 320px (iPhone SE)
- [x] Test at 375px (iPhone 12)
- [x] Test at 768px (iPad Portrait)
- [x] Test at 1024px (iPad Landscape)
- [x] Test at 1280px (Desktop min)
- [x] Test at 1920px (Desktop large)
- [x] Test landscape orientations
- [x] Test light/dark themes
- [x] Test with long content
- [x] Test expand/collapse at each size

---

## Browser Support

✅ **Modern Browsers:**

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

✅ **CSS Features Used:**

- Flexbox (universal support)
- CSS transforms (universal support)
- CSS transitions (universal support)
- CSS custom properties (universal support)

❌ **Not Supported:**

- IE11 (end of life)

---

**Created:** October 31, 2025  
**Last Updated:** October 31, 2025
