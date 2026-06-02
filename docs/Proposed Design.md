---
name: Scholastic Paper
colors:
  surface: '#fbf9f4'
  surface-dim: '#dcdad5'
  surface-bright: '#fbf9f4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3ee'
  surface-container: '#f0eee9'
  surface-container-high: '#eae8e3'
  surface-container-highest: '#e4e2dd'
  on-surface: '#1b1c19'
  on-surface-variant: '#414942'
  inverse-surface: '#30312e'
  inverse-on-surface: '#f2f1ec'
  outline: '#717971'
  outline-variant: '#c1c9c0'
  surface-tint: '#3b684a'
  primary: '#144227'
  on-primary: '#ffffff'
  primary-container: '#2d5a3d'
  on-primary-container: '#9ed0ab'
  inverse-primary: '#a1d2ad'
  secondary: '#88520e'
  on-secondary: '#ffffff'
  secondary-container: '#feb56b'
  on-secondary-container: '#774400'
  tertiary: '#780a13'
  on-tertiary: '#ffffff'
  tertiary-container: '#992527'
  on-tertiary-container: '#ffb0aa'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#bceec8'
  primary-fixed-dim: '#a1d2ad'
  on-primary-fixed: '#00210f'
  on-primary-fixed-variant: '#224f33'
  secondary-fixed: '#ffdcbe'
  secondary-fixed-dim: '#ffb870'
  on-secondary-fixed: '#2d1600'
  on-secondary-fixed-variant: '#693c00'
  tertiary-fixed: '#ffdad7'
  tertiary-fixed-dim: '#ffb3ae'
  on-tertiary-fixed: '#410004'
  on-tertiary-fixed-variant: '#8a1a1d'
  background: '#fbf9f4'
  on-background: '#1b1c19'
  surface-variant: '#e4e2dd'
  ink-primary: '#1a1a18'
  ink-secondary: '#6b6b66'
  ink-tertiary: '#a0a099'
  paper-white: '#ffffff'
  paper-muted: '#eceae3'
  border-soft: '#e2dfd7'
  border-strong: '#c8c5bc'
  info-blue: '#185fa5'
  notification-red: '#e24b4a'
typography:
  headline-lg:
    fontFamily: Lora
    fontSize: 21px
    fontWeight: '500'
    lineHeight: 32px
  headline-md:
    fontFamily: Lora
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  title-card:
    fontFamily: Lora
    fontSize: 13.5px
    fontWeight: '500'
    lineHeight: 18px
  title-grid:
    fontFamily: Lora
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  body-default:
    fontFamily: DM Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: DM Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-caps:
    fontFamily: DM Sans
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.9px
  meta:
    fontFamily: DM Sans
    fontSize: 11.5px
    fontWeight: '400'
    lineHeight: 16px
  badge:
    fontFamily: DM Sans
    fontSize: 10.5px
    fontWeight: '500'
    lineHeight: 12px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  sidebar-width: 204px
  search-max-width: 540px
  section-gap: 22px
  container-pad: 24px
  item-pad: 12px
  grid-gap: 14px
---

## Brand & Style

This design system is built for the modern academic environment, blending the intellectual heritage of traditional libraries with the efficiency of digital management. The aesthetic is **warm, scholarly, and structured**, prioritizing information density without sacrificing breathing room.

The design draws from **Minimalism** and **Tactile** movements. It evokes the sensation of physical paper and cloth-bound books through a "warm paper" base and a forest-inspired accent palette. The interface avoids aggressive digital shadows, instead using precise borders and subtle tonal layering to create a sense of organized depth. The emotional response is one of calm authority and reliability.

## Colors

The palette is rooted in a "warm paper" philosophy. The primary background (`#f4f2ed`) mimics a physical catalog card, while interactive surfaces use pure white to provide clean contrast.

### Color Roles
- **Primary (Forest Green):** Used for branding, active states, and "success" indicators (e.g., "Available").
- **Secondary (Amber):** Reserved for warnings and items requiring attention soon.
- **Tertiary (Deep Red):** Specifically for overdue status, errors, or critical alerts.
- **Neutral (Ink/Paper):** A range of warm grays and off-whites that handle all structural and typographic duties. 

Avoid using pure black (`#000000`); use `ink-primary` for all high-contrast text to maintain the academic warmth.

## Typography

This system uses a classical **Serif (Lora)** for discovery and a modern **Sans (DM Sans)** for utility.

- **Lora** is used for "content" elements: book titles, main greetings, and the logo. This reinforces the literary nature of the product.
- **DM Sans** is used for "interface" elements: navigation, labels, data points, and buttons. It provides the necessary clarity for complex management tasks.

For mobile screens, maintain the `headline-lg` size but ensure book titles (`title-card`) do not drop below `13px` to preserve legibility in dense lists.

## Layout & Spacing

The layout follows a **Fixed-Fluid Hybrid** model. The sidebar remains a constant 204px for structural reliability, while the main content area utilizes a fluid grid that optimizes for readability.

### Layout Rules:
- **Grid Systems:** Use a 5-column grid for book cover displays and a 4-column grid for dashboard action cards.
- **Rhythm:** Spacing is strictly based on a 4px/2px increment system.
- **Breakpoints:**
  - **Desktop (1024px+):** Full sidebar visible; 24px container padding.
  - **Tablet (768px - 1023px):** Sidebar may collapse to icons; grid shifts to 3-column.
  - **Mobile (<767px):** Full-screen width; sidebar moves to a bottom-nav or hamburger menu; 16px container padding.

## Elevation & Depth

The system uses **Tonal Layers and Low-Contrast Outlines** rather than traditional shadows to convey hierarchy. 

1.  **Level 0 (Base):** The `--bg` (`#f4f2ed`) represents the desk surface.
2.  **Level 1 (Surface):** Cards, inputs, and sidebars use pure white (`#ffffff`) with a 1px `--border-soft` to appear "resting" on the base.
3.  **Level 2 (Interactive):** Hover states are indicated by a shift to `--paper-muted` or a strengthening of the border to `--border-strong`.
4.  **Sticky Elements:** The topbar uses `z-index: 100` and a subtle bottom border to signify its position above the scrolling content.

## Shapes

The shape language is categorized by the "organic" vs. "functional" nature of the element:

- **Functional Elements:** Search inputs, action buttons, and notifications use a standard **8px (rounded)** radius.
- **Container Elements:** Content cards and list containers use a larger **12px (rounded-lg)** radius to create a softer, more inviting frame for scholarly content.
- **Content:** Book covers use a tighter **5px** radius to mimic the slightly rounded corners of a physical book spine or cover.
- **Identity:** Avatars and status indicators (dots) are always **50% (circular)**.

## Components

### Buttons & Inputs
- **Search Input:** 540px max-width, 8px radius, `--paper-muted` background. On focus, the border transitions to `--border-strong` over 0.15s.
- **Action Buttons:** Use `--grn-bg` for secondary actions and `--grn` with white text for primary calls to action.

### Cards
- **Book Cards:** Feature a 5px rounded cover image. Titles use `title-card` (Lora) and authors use `meta` (DM Sans). On hover, the image opacity shifts to 0.85.
- **Quick Action Cards:** 30px square icon containers with semantic backgrounds (e.g., `grn-bg`). 12px rounded corners for the outer card.

### Badges & Status
- **Semantic Badges:** Small, rounded-pill shapes. Use the "bg" tint for the surface and the "text" tint for the label (e.g., `--amb-bg` surface with `--amb-text` for "Due Soon").
- **Notification Dots:** 8px circles in `notification-red` for global alerts or semantic colors (green, amber, red) for status-specific indicators in the user greeting section.

### Navigation
- **Sidebar Links:** 13px DM Sans. Active states should use `--grn-bg` with `--grn-text` and a 500 font weight. High-level category labels should be all-caps with 0.9px letter spacing.