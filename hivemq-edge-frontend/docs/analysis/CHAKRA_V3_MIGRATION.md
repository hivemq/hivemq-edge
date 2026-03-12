---
title: "Chakra UI v3 Migration Analysis"
author: "Edge Frontend Team"
last_updated: "2025-12-12"
purpose: "Cost-benefit analysis and phased migration plan for upgrading from Chakra UI v2 to v3"
audience: "Technical leadership and frontend developers evaluating migration timeline and risk"
maintained_at: "docs/analysis/CHAKRA_V3_MIGRATION.md"
---

# Chakra UI v3 Migration Analysis

---

## Table of Contents

- [Executive Summary](#executive-summary)
- [Breaking Changes Analysis](#breaking-changes-analysis)
  - [Architecture Changes](#architecture-changes)
  - [Component API Changes](#component-api-changes)
  - [Removed and Replaced APIs](#removed-and-replaced-apis)
- [Codebase Impact Analysis](#codebase-impact-analysis)
  - [Package Dependencies](#package-dependencies)
  - [File Impact by Area](#file-impact-by-area)
  - [High-Impact Patterns](#high-impact-patterns)
- [Third-Party Dependencies Risk](#third-party-dependencies-risk)
- [Migration Strategy](#migration-strategy)
  - [Phased Approach](#phased-approach)
  - [Total Timeline](#total-timeline)
- [Component Migration Reference](#component-migration-reference)
  - [High-Frequency Components](#high-frequency-components)
  - [Theme Migration](#theme-migration)
- [Risk Assessment](#risk-assessment)
- [Resource Requirements](#resource-requirements)
- [Recommendations](#recommendations)
- [Glossary](#glossary)
- [Related Documentation](#related-documentation)

---

## Executive Summary

Migrating from Chakra UI v2.8.2 to v3 is a **significant undertaking** due to the architectural changes in v3. Based on codebase analysis and Chakra's migration documentation, the estimated effort is **10-14 weeks** for a complete migration with proper testing.

| Metric | Value |
|--------|-------|
| **Files using Chakra** | ~300+ TSX files |
| **Component themes** | 7 custom theme files |
| **Third-party Chakra deps** | 4 packages |
| **RJSF custom components** | 20+ widgets/templates |
| **Estimated effort** | 10-14 weeks |
| **Risk level** | Medium-High |
| **Critical blocker** | RJSF + Chakra v3 compatibility |

**Key finding:** The migration is technically feasible, but the `@rjsf/chakra-ui` dependency is the highest-risk item — RJSF v6 with Chakra v3 support must be validated before committing to the full migration. Run a 2-week pre-migration validation before beginning any full-scale work.

---

## Breaking Changes Analysis

### Architecture Changes

The v3 theme system is a complete rewrite — there is no incremental migration path for the theme layer:

| v2 Concept | v3 Replacement | Impact |
|------------|----------------|--------|
| `ChakraProvider` | `ChakraProvider` + system config | Configuration changes |
| `extendTheme()` | `createSystem()` + `defaultConfig` | **Full theme rewrite** |
| `@chakra-ui/anatomy` | Built-in slot recipes | **Full theme rewrite** |
| `@chakra-ui/theme-tools` | Removed entirely | **Code changes** |
| `@chakra-ui/icons` | Bring your own icons | **Icon migration** |
| `useColorModeValue()` | `colorPalette` or CSS variables | **50+ usages** |
| `defineStyleConfig` | `defineRecipe` / `defineSlotRecipe` | **Full theme rewrite** |

### Component API Changes

The overlay and disclosure components move to a compound component pattern, requiring structural JSX rewrites:

| Component | Change Type | Current Usage |
|-----------|-------------|---------------|
| **Button** | Variant naming, icon props | Extensive (~100+ usages) |
| **Drawer** | Compound component pattern | 15+ usages |
| **Modal** | Compound component pattern | 10+ usages |
| **Tabs** | Compound component pattern | 8+ usages |
| **Menu** | Compound component pattern | 10+ usages |
| **Alert** | Compound component pattern | 10+ usages |
| **Card** | New compound pattern | 20+ usages |
| **Form** | Field/Control restructured | 50+ usages |
| **Skeleton** | API changes | 15+ usages |
| **Toast** | `useToast` → `toaster` singleton | 20+ usages |
| **Tooltip** | Props changed | 15+ usages |

### Removed and Replaced APIs

| v2 API | v3 Replacement | Files Affected |
|--------|----------------|----------------|
| `useDisclosure` | Still available | 30+ files |
| `useColorModeValue` | CSS color-mix or `colorPalette` | 20+ files |
| `useToken` | `token()` function | 5+ files |
| `useTheme` | `useChakraContext` | 3+ files |
| `createMultiStyleConfigHelpers` | `defineSlotRecipe` | 5 theme files |

---

## Codebase Impact Analysis

### Package Dependencies

| Package | Current Version | v3 Status | Migration Path |
|---------|----------------|-----------|----------------|
| `@chakra-ui/react` | 2.8.2 | ✅ Upgrade | Core migration |
| `@chakra-ui/anatomy` | 2.2.2 | ❌ Removed | Use slot recipes |
| `@chakra-ui/icons` | 2.1.1 | ❌ Removed | Use `react-icons` |
| `@chakra-ui/theme-tools` | 2.2.9 | ❌ Removed | Inline utilities |
| `@chakra-ui/skip-nav` | 2.1.0 | ⚠️ Check | May need replacement |
| `@chakra-ui/radio` | 2.1.2 | ✅ Merged | Use main package |
| `@chakra-ui/utils` | 2.0.14 | ⚠️ Reduced | Check each usage |
| `chakra-react-select` | 4.7.6 | ⚠️ Check | v3 compatibility TBD |
| `@rjsf/chakra-ui` | 5.24.13 | ⚠️ Critical | RJSF v6 needed |

### File Impact by Area

| Area | Files | Chakra Usage | Complexity |
|------|-------|--------------|------------|
| **Shared Components** | ~50 | Heavy | High |
| **RJSF Components** | ~25 | Heavy | **Critical** |
| **DataHub** | ~80 | Medium | Medium |
| **Workspace** | ~60 | Medium | Medium |
| **Modules (other)** | ~100 | Heavy | High |
| **Theme** | ~10 | Core | **Critical** |

### High-Impact Patterns

#### `useColorModeValue` (~50 usages in 20+ files)

```typescript
// v2 (current)
const bgColour = useColorModeValue('gray.300', 'gray.900')

// v3 — Option 1: CSS color-mix (inline)
<Box bg={{ base: 'gray.300', _dark: 'gray.900' }} />

// v3 — Option 2: colorPalette
<Box colorPalette="gray" bg="colorPalette.300" />
```

#### Custom Theme Components (7 files in `src/modules/Theme/`)

```typescript
// v2 (current) — Button.ts
import { defineStyleConfig } from '@chakra-ui/react'
export const buttonTheme = defineStyleConfig({
  variants: { primary, danger },
})

// v3 — button.recipe.ts
import { defineRecipe } from '@chakra-ui/react'
export const buttonRecipe = defineRecipe({
  variants: {
    visual: { primary: { ... }, danger: { ... } }
  },
})
```

#### Drawer/Modal Compound Pattern (~25 combined usages)

```typescript
// v2 (current)
<Drawer isOpen={isOpen} onClose={onClose} placement="right" size="lg">
  <DrawerOverlay />
  <DrawerContent>
    <DrawerCloseButton />
    <DrawerHeader>Title</DrawerHeader>
    <DrawerBody>Content</DrawerBody>
    <DrawerFooter>Footer</DrawerFooter>
  </DrawerContent>
</Drawer>

// v3 — compound pattern
<Drawer.Root open={isOpen} onOpenChange={(e) => !e.open && onClose()} placement="end" size="lg">
  <Drawer.Backdrop />
  <Drawer.Positioner>
    <Drawer.Content>
      <Drawer.CloseTrigger asChild><CloseButton /></Drawer.CloseTrigger>
      <Drawer.Header>Title</Drawer.Header>
      <Drawer.Body>Content</Drawer.Body>
      <Drawer.Footer>Footer</Drawer.Footer>
    </Drawer.Content>
  </Drawer.Positioner>
</Drawer.Root>
```

#### Toast (~20+ usages)

```typescript
// v2 (current)
const toast = useToast()
toast({ title: 'Success', status: 'success', duration: 3000 })

// v3
import { toaster } from '@/components/ui/toaster'
toaster.success({ title: 'Success', duration: 3000 })
```

---

## Third-Party Dependencies Risk

### `chakra-react-select` — Medium-High Risk

| Item | Detail |
|------|--------|
| Current version | 4.7.6 |
| Risk | v3 compatibility TBD |
| Files affected | 12+ |

Options:
1. Wait for maintainer to release v3-compatible version
2. Fork and update (significant effort)
3. Replace with native `<Select>` or headless `react-select` + custom styling

### `@rjsf/chakra-ui` — Critical Risk

| Item | Detail |
|------|--------|
| Current version | 5.24.13 |
| Risk | **Critical — blocks entire RJSF layer** |
| Files affected | All RJSF forms (~50+ locations) |

Options:
1. Wait for official `@rjsf/chakra-ui` v6 with Chakra v3 support (**preferred if timeline allows**)
2. Create a custom RJSF theme targeting Chakra v3 (~2-3 weeks additional effort)
3. Use headless RJSF with custom Chakra v3 widgets (highest effort, most flexibility)

**Validate this dependency in the pre-migration phase before beginning full-scale work.**

---

## Migration Strategy

### Phased Approach

#### Phase 0: Pre-Migration Validation (1-2 weeks)

| Task | Effort | Risk |
|------|--------|------|
| Audit all Chakra imports | 2d | Low |
| Test `@rjsf/chakra-ui` v6 + Chakra v3 compatibility | 2d | High |
| Test `chakra-react-select` v3 compatibility | 2d | Medium |
| Theme migration prototype (Button + Drawer) | 2d | Medium |
| Go/No-Go decision | 1d | — |

#### Phase 1: Foundation (2-3 weeks)

| Task | Effort | Dependencies |
|------|--------|--------------|
| Migrate theme to `createSystem()` | 5d | None |
| Convert component recipes | 3d | Theme |
| Replace `@chakra-ui/icons` with `react-icons` | 2d | None |
| Remove `@chakra-ui/theme-tools` usages | 1d | None |
| Update `ChakraProvider` setup | 1d | Theme |

#### Phase 2: Component Migration (3-4 weeks)

| Component Group | Files | Effort |
|----------------|-------|--------|
| Layout (`Box`, `Flex`, `Grid`, `Stack`) | ~200 | 2d (mostly compatible) |
| Typography (`Text`, `Heading`) | ~150 | 1d (mostly compatible) |
| Forms (`Input`, `Select`, `Checkbox`) | ~80 | 3d |
| Feedback (`Alert`, `Toast`, `Spinner`) | ~30 | 2d |
| Overlay (`Modal`, `Drawer`, `Popover`) | ~35 | 4d (compound pattern) |
| Disclosure (`Tabs`, `Accordion`) | ~20 | 2d (compound pattern) |
| Navigation (`Menu`, `Breadcrumb`) | ~15 | 2d |
| Data Display (`Table`, `Card`, `Badge`) | ~50 | 3d |

#### Phase 3: RJSF Migration (2-3 weeks)

| Task | Effort | Risk |
|------|--------|------|
| Upgrade to RJSF v6 | 3d | Medium |
| Migrate `ChakraRJSForm` wrapper | 2d | Medium |
| Update all custom widgets (6) | 4d | Medium |
| Update all custom templates (8) | 4d | Medium |
| Update all custom fields (3) | 2d | Medium |
| Test all forms | 3d | High |

#### Phase 4: Testing and Stabilisation (2 weeks)

| Task | Effort |
|------|--------|
| Fix visual regressions | 5d |
| Update component tests | 3d |
| Update E2E tests | 2d |
| Accessibility audit | 2d |

### Total Timeline

| Phase | Duration | Cumulative |
|-------|----------|------------|
| Phase 0: Validation | 1-2 weeks | 1-2 weeks |
| Phase 1: Foundation | 2-3 weeks | 3-5 weeks |
| Phase 2: Components | 3-4 weeks | 6-9 weeks |
| Phase 3: RJSF | 2-3 weeks | 8-12 weeks |
| Phase 4: Testing | 2 weeks | 10-14 weeks |

---

## Component Migration Reference

### High-Frequency Components

#### Button (~100+ usages)

```typescript
// v2
<Button variant="primary" leftIcon={<Icon />}>Click</Button>

// v3 — icon becomes a child, variant uses recipe
<Button variant="solid" colorPalette="yellow">
  <Icon /> Click
</Button>
```

**Changes:** Custom `primary` variant uses a recipe; `leftIcon`/`rightIcon` props removed — icons are children.

#### Tabs (~8 usages)

```typescript
// v2
<Tabs>
  <TabList>
    <Tab>One</Tab>
    <Tab>Two</Tab>
  </TabList>
  <TabPanels>
    <TabPanel>Content 1</TabPanel>
    <TabPanel>Content 2</TabPanel>
  </TabPanels>
</Tabs>

// v3 — value-based, compound pattern
<Tabs.Root>
  <Tabs.List>
    <Tabs.Trigger value="one">One</Tabs.Trigger>
    <Tabs.Trigger value="two">Two</Tabs.Trigger>
  </Tabs.List>
  <Tabs.Content value="one">Content 1</Tabs.Content>
  <Tabs.Content value="two">Content 2</Tabs.Content>
</Tabs.Root>
```

### Theme Migration

#### Current Theme Structure (`src/modules/Theme/`)

```
src/modules/Theme/
├── themeHiveMQ.ts          # Main theme (extendTheme)
├── components/
│   ├── Alert.ts            # createMultiStyleConfigHelpers
│   ├── Button.ts           # defineStyleConfig
│   ├── Drawer.ts           # createMultiStyleConfigHelpers
│   ├── FormControl.ts      # createMultiStyleConfigHelpers
│   ├── FormErrorMessage.ts # createMultiStyleConfigHelpers
│   ├── Spinner.ts          # defineStyleConfig
│   └── Stat.ts             # createMultiStyleConfigHelpers
├── foundations/
│   └── colors.ts           # Color tokens
└── globals/
    ├── react-flow.ts        # Global styles
    └── treeview.ts          # Global styles
```

#### Target v3 Theme Structure

```
src/modules/Theme/
├── theme.ts                # createSystem + defaultConfig
├── recipes/
│   ├── alert.recipe.ts     # defineSlotRecipe
│   ├── button.recipe.ts    # defineRecipe
│   ├── drawer.recipe.ts    # defineSlotRecipe
│   └── ...
├── tokens/
│   └── colors.ts           # Semantic tokens
└── globals/
    └── styles.ts            # Global CSS
```

---

## Risk Assessment

### Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| `@rjsf/chakra-ui` not ready for Chakra v3 | Medium | **Critical** | Custom theme or wait for v6 |
| `chakra-react-select` incompatible | Medium | High | Fork or replace |
| Visual regressions across 300+ files | High | Medium | Percy visual regression testing |
| Accessibility regressions | Medium | High | Axe testing on every PR |
| Dark mode issues | High | Medium | Thorough component testing |

### Schedule Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Underestimated component changes | Medium | Medium | Buffer time in estimates |
| Third-party deps block migration | Medium | High | Validate in Phase 0 |
| Testing reveals widespread regressions | High | Medium | Continuous testing from Phase 1 |

---

## Resource Requirements

### Team Allocation

| Role | Allocation | Duration |
|------|------------|----------|
| Senior Frontend Dev | 100% | 10-14 weeks |
| Frontend Dev | 50% | 10-14 weeks |
| QA Engineer | 25% | 8-10 weeks |
| Designer (review) | 10% | 4-6 weeks |

### Required Tooling

| Tool | Purpose |
|------|---------|
| Chakra UI CLI | Code codemods (partial automation only) |
| Percy/Chromatic | Visual regression testing before/after |
| Cypress | Component + E2E regression suite |
| axe-core | Accessibility regression detection |

---

## Recommendations

### Phase 0 is Non-Negotiable

Before committing to the full migration:

1. **Validate `@rjsf/chakra-ui` v6 + Chakra v3 compatibility** — this is the highest-risk dependency. Build a proof-of-concept form with custom widgets before proceeding.
2. **Validate `chakra-react-select` v3 support** — test with actual select components, evaluate replacement options if needed.
3. **Build a theme migration prototype** — convert `Button` + `Drawer` themes to validate dark mode and variant behavior.
4. **Make a formal Go/No-Go decision** after Phase 0 results are known.

### Hybrid Approach (If Running in Parallel with UX Work)

If combining with a resource listings UX migration:

| Option | Description | Advantage | Risk |
|--------|-------------|-----------|------|
| **Parallel tracks** | Chakra v3 + new resource components simultaneously | New components built in v3 from day one | Larger parallel effort |
| **Sequential** | Complete Chakra v3, then add resource UX | Lower complexity per phase | Longer total timeline (16-23 weeks) |

### Fallback Plan

- **RJSF blocker:** Wait for official `@rjsf/chakra-ui` v6, or build a custom RJSF theme targeting Chakra v3.
- **`chakra-react-select` blocker:** Replace with headless `react-select` + custom Chakra v3 styling.
- **Timeline overrun:** Pause migration at a natural boundary (for example, after shared components) to deliver partial value.

---

## Glossary

| Term | Definition |
|------|------------|
| **Chakra UI** | React component library providing accessible UI primitives with a built-in theme system |
| **Component Recipe** | Chakra v3 replacement for `defineStyleConfig` — defines a single-element component's variants and styles |
| **Slot Recipe** | Chakra v3 replacement for `createMultiStyleConfigHelpers` — defines styles for multi-element components (for example, Drawer with Root/Content/Header parts) |
| **Compound Component Pattern** | v3 architectural pattern where components are composed via sub-components (for example, `Drawer.Root`, `Drawer.Body`) rather than a monolithic wrapper |
| **colorPalette** | Chakra v3 replacement for `colorScheme` — semantic palette prop applied to component and all its parts |
| **createSystem** | Chakra v3 equivalent of `extendTheme` — creates a typed design system from token and recipe configuration |
| **RJSF** | React JSON Schema Form (`@rjsf/core`) — form generation library used for protocol adapter and bridge configuration; depends on `@rjsf/chakra-ui` for Chakra theming |
| **defineRecipe** | Chakra v3 API for defining single-slot component style variants |
| **defineSlotRecipe** | Chakra v3 API for defining multi-slot component style variants (replaces `createMultiStyleConfigHelpers`) |
| **useColorModeValue** | Chakra v2 hook for dark/light mode conditional styling — replaced by CSS `_dark` props or `colorPalette` in v3 |
| **toaster** | Chakra v3 singleton replacing the `useToast` hook for programmatic toast notifications |
| **Visual Regression** | Automated screenshot comparison to detect unintended UI changes between code versions |

---

## Related Documentation

**Architecture:**
- [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md)
- [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md)
- [Protocol Adapter Architecture](../architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md)

**Guides:**
- [Design Guide](../guides/DESIGN_GUIDE.md) - Current button variants and component patterns
- [RJSF Guide](../guides/RJSF_GUIDE.md) - RJSF widget implementation (all affected by migration)
- [Testing Guide](../guides/TESTING_GUIDE.md) - Testing strategy for regression validation

**Technical:**
- [Technical Stack](../technical/TECHNICAL_STACK.md) - Current dependency versions and toolchain
