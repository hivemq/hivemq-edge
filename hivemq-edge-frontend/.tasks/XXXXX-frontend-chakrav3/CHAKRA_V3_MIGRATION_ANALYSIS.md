# Chakra UI V3 Migration Analysis

**Document Purpose:** Quantified analysis of migrating HiveMQ Edge frontend from Chakra UI v2 to v3, with timeline, cost, and phased plan.

**Date:** December 12, 2025

---

## Executive Summary

Migrating from Chakra UI v2.8.2 to v3 is a **significant undertaking** due to the architectural changes in v3. Based on codebase analysis and Chakra's migration documentation, the estimated effort is **8-12 weeks** for a complete migration with proper testing.

| Metric                      | Value                 |
| :-------------------------- | :-------------------- |
| **Files using Chakra**      | \~300+ TSX files      |
| **Component themes**        | 7 custom theme files  |
| **Third-party Chakra deps** | 4 packages            |
| **RJSF custom components**  | 20+ widgets/templates |
| **Estimated effort**        | 8-12 weeks            |

---

## 1\. Chakra V3 Breaking Changes Analysis

Based on [Chakra UI v3 Migration Documentation](https://chakra-ui.com/llms-v3-migration.txt):

### 1.1 Architecture Changes

| V2 Concept               | V3 Replacement                      | Impact             |
| :----------------------- | :---------------------------------- | :----------------- |
| `ChakraProvider`         | `ChakraProvider` \+ system          | Config changes     |
| `extendTheme()`          | `createSystem()` \+ `defaultConfig` | **Theme rewrite**  |
| `@chakra-ui/anatomy`     | Built-in slot recipes               | **Theme rewrite**  |
| `@chakra-ui/theme-tools` | Removed                             | **Code changes**   |
| `@chakra-ui/icons`       | Bring your own icons                | **Icon migration** |
| `useColorModeValue()`    | `colorPalette` or CSS variables     | **50+ usages**     |
| `defineStyleConfig`      | `defineRecipe` / `defineSlotRecipe` | **Theme rewrite**  |

### 1.2 Component API Changes

| Component    | Change Type                | Current Usage |
| :----------- | :------------------------- | :------------ |
| **Button**   | Variant naming, props      | Extensive     |
| **Drawer**   | Compound component pattern | 15+ usages    |
| **Modal**    | Compound component pattern | 10+ usages    |
| **Tabs**     | Compound component pattern | 8+ usages     |
| **Menu**     | Compound component pattern | 10+ usages    |
| **Alert**    | Compound component pattern | 10+ usages    |
| **Card**     | New compound pattern       | 20+ usages    |
| **Form**     | Field/Control restructured | 50+ usages    |
| **Skeleton** | API changes                | 15+ usages    |
| **Toast**    | New API                    | 20+ usages    |
| **Tooltip**  | Props changed              | 15+ usages    |

### 1.3 Removed/Replaced Components

| V2 Component                    | V3 Replacement                  | Files Affected |
| :------------------------------ | :------------------------------ | :------------- |
| `useDisclosure`                 | Still available                 | 30+ files      |
| `useColorModeValue`             | CSS color-mix or `colorPalette` | 20+ files      |
| `useToken`                      | `token()` function              | 5+ files       |
| `useTheme`                      | `useChakraContext`              | 3+ files       |
| `createMultiStyleConfigHelpers` | `defineSlotRecipe`              | 5 theme files  |

---

## 2\. Current Codebase Impact Analysis

### 2.1 Chakra Package Dependencies

| Package                  | Version | V3 Status   | Migration            |
| :----------------------- | :------ | :---------- | :------------------- |
| `@chakra-ui/react`       | 2.8.2   | ✅ Upgrade  | Core migration       |
| `@chakra-ui/anatomy`     | 2.2.2   | ❌ Removed  | Use slot recipes     |
| `@chakra-ui/icons`       | 2.1.1   | ❌ Removed  | Use `react-icons`    |
| `@chakra-ui/theme-tools` | 2.2.9   | ❌ Removed  | Inline utilities     |
| `@chakra-ui/skip-nav`    | 2.1.0   | ⚠️ Check    | May need replacement |
| `@chakra-ui/radio`       | 2.1.2   | ✅ Merged   | Use main package     |
| `@chakra-ui/utils`       | 2.0.14  | ⚠️ Reduced  | Check each usage     |
| `chakra-react-select`    | 4.7.6   | ⚠️ Check    | V3 compatibility     |
| `@rjsf/chakra-ui`        | 5.24.13 | ⚠️ Critical | RJSF v6 needed       |

### 2.2 File Impact by Area

| Area                  | Files | Chakra Usage | Complexity   |
| :-------------------- | :---- | :----------- | :----------- |
| **Shared Components** | \~50  | Heavy        | High         |
| **RJSF Components**   | \~25  | Heavy        | **Critical** |
| **DataHub**           | \~80  | Medium       | Medium       |
| **Workspace**         | \~60  | Medium       | Medium       |
| **Modules (other)**   | \~100 | Heavy        | High         |
| **Theme**             | \~10  | Core         | **Critical** |

### 2.3 High-Impact Patterns

#### useColorModeValue (50+ usages)

```ts
// V2 (current)
const bgColour = useColorModeValue('gray.300', 'gray.900')

// V3 (new)
// Option 1: CSS color-mix
<Box bg={{ base: 'gray.300', _dark: 'gray.900' }} />

// Option 2: colorPalette
<Box colorPalette="gray" bg="colorPalette.300" />
```

**Files affected:** 20+ files with 50+ usages

#### Custom Theme Components (7 files)

```ts
// V2 (current) - Button.ts
import { defineStyleConfig } from '@chakra-ui/react'
export const buttonTheme = defineStyleConfig({
  variants: { primary, danger },
})

// V3 (new)
import { defineRecipe } from '@chakra-ui/react'
export const buttonRecipe = defineRecipe({
  variants: {
    visual: { primary: {...}, danger: {...} }
  },
})
```

#### Drawer/Modal Compound Pattern

```ts
// V2 (current)
<Drawer isOpen={isOpen} onClose={onClose}>
  <DrawerOverlay />
  <DrawerContent>
    <DrawerCloseButton />
    <DrawerHeader>Title</DrawerHeader>
    <DrawerBody>Content</DrawerBody>
    <DrawerFooter>Footer</DrawerFooter>
  </DrawerContent>
</Drawer>

// V3 (new) - Compound pattern
<Drawer.Root open={isOpen} onOpenChange={(e) => !e.open && onClose()}>
  <Drawer.Backdrop />
  <Drawer.Positioner>
    <Drawer.Content>
      <Drawer.CloseTrigger />
      <Drawer.Header>Title</Drawer.Header>
      <Drawer.Body>Content</Drawer.Body>
      <Drawer.Footer>Footer</Drawer.Footer>
    </Drawer.Content>
  </Drawer.Positioner>
</Drawer.Root>
```

**Files affected:** 15+ drawer usages, 10+ modal usages

---

## 3\. Third-Party Dependencies Risk

### 3.1 chakra-react-select

| Status          | Risk        |
| :-------------- | :---------- |
| Current: v4.7.6 | Medium-High |

The package may not immediately support Chakra V3. Options:

1. Wait for update
2. Fork and update
3. Replace with native `Select` or `react-select` \+ custom styling

**Files affected:** 12+ files

### 3.2 @rjsf/chakra-ui

| Status            | Risk         |
| :---------------- | :----------- |
| Current: v5.24.13 | **Critical** |

RJSF Chakra theme must be updated for V3. Options:

1. Wait for official `@rjsf/chakra-ui` v6 with V3 support
2. Create custom theme based on V3
3. Use headless RJSF with custom Chakra V3 widgets

**Files affected:** All RJSF forms (\~50+ locations)

---

## 4\. Migration Strategy

### 4.1 Phased Approach (Recommended)

#### Phase 0: Preparation (1-2 weeks)

| Task                                      | Effort | Risk   |
| :---------------------------------------- | :----- | :----- |
| Audit all Chakra imports                  | 2d     | Low    |
| Create migration tracking sheet           | 1d     | Low    |
| Set up parallel V3 branch                 | 1d     | Low    |
| Test chakra-react-select V3 compatibility | 2d     | Medium |
| Check RJSF V6 \+ Chakra V3 status         | 2d     | High   |

#### Phase 1: Foundation (2-3 weeks)

| Task                                          | Effort | Dependencies |
| :-------------------------------------------- | :----- | :----------- |
| Migrate theme system to `createSystem()`      | 5d     | None         |
| Convert component recipes                     | 3d     | Theme        |
| Replace `@chakra-ui/icons` with `react-icons` | 2d     | None         |
| Remove `@chakra-ui/theme-tools` usages        | 1d     | None         |
| Update `ChakraProvider` setup                 | 1d     | Theme        |

#### Phase 2: Component Migration (3-4 weeks)

| Component Group                   | Files | Effort                 |
| :-------------------------------- | :---- | :--------------------- |
| Layout (Box, Flex, Grid, Stack)   | \~200 | 2d (mostly compatible) |
| Typography (Text, Heading)        | \~150 | 1d (mostly compatible) |
| Forms (Input, Select, Checkbox)   | \~80  | 3d                     |
| Feedback (Alert, Toast, Spinner)  | \~30  | 2d                     |
| Overlay (Modal, Drawer, Popover)  | \~35  | 4d (compound pattern)  |
| Disclosure (Tabs, Accordion)      | \~20  | 2d (compound pattern)  |
| Navigation (Menu, Breadcrumb)     | \~15  | 2d                     |
| Data Display (Table, Card, Badge) | \~50  | 3d                     |

#### Phase 3: RJSF Migration (2-3 weeks)

| Task                            | Effort | Risk   |
| :------------------------------ | :----- | :----- |
| Upgrade to RJSF v6              | 3d     | Medium |
| Migrate ChakraRJSForm wrapper   | 2d     | Medium |
| Update all custom widgets (6)   | 4d     | Medium |
| Update all custom templates (8) | 4d     | Medium |
| Update all custom fields (3)    | 2d     | Medium |
| Test all forms                  | 3d     | High   |

#### Phase 4: Testing & Fixes (2 weeks)

| Task                   | Effort |
| :--------------------- | :----- |
| Fix visual regressions | 5d     |
| Update component tests | 3d     |
| Update E2E tests       | 2d     |
| Accessibility audit    | 2d     |

### 4.2 Total Timeline

| Phase                | Duration  | Cumulative  |
| :------------------- | :-------- | :---------- |
| Phase 0: Preparation | 1-2 weeks | 1-2 weeks   |
| Phase 1: Foundation  | 2-3 weeks | 3-5 weeks   |
| Phase 2: Components  | 3-4 weeks | 6-9 weeks   |
| Phase 3: RJSF        | 2-3 weeks | 8-12 weeks  |
| Phase 4: Testing     | 2 weeks   | 10-14 weeks |

**Total: 10-14 weeks** (with buffer for unknowns)

---

## 5\. Component-by-Component Migration Guide

### 5.1 High-Frequency Components

#### Button (\~100+ usages)

```ts
// V2
<Button variant="primary" leftIcon={<Icon />}>Click</Button>

// V3
<Button variant="solid" colorPalette="yellow">
  <Icon /> Click
</Button>
```

**Changes:**

- Custom `primary` variant → use recipe
- `leftIcon`/`rightIcon` → use children with Icon

#### Drawer (\~15 usages)

```ts
// V2
<Drawer isOpen onClose={fn} placement="right" size="lg">
  <DrawerOverlay />
  <DrawerContent>
    <DrawerCloseButton />
    <DrawerHeader>Title</DrawerHeader>
    <DrawerBody>...</DrawerBody>
    <DrawerFooter>...</DrawerFooter>
  </DrawerContent>
</Drawer>

// V3
<Drawer.Root open onOpenChange={fn} placement="end" size="lg">
  <Drawer.Backdrop />
  <Drawer.Positioner>
    <Drawer.Content>
      <Drawer.CloseTrigger asChild>
        <CloseButton />
      </Drawer.CloseTrigger>
      <Drawer.Header>Title</Drawer.Header>
      <Drawer.Body>...</Drawer.Body>
      <Drawer.Footer>...</Drawer.Footer>
    </Drawer.Content>
  </Drawer.Positioner>
</Drawer.Root>
```

**Changes:**

- `isOpen` → `open`
- `onClose` → `onOpenChange`
- `placement="right"` → `placement="end"`
- Compound component pattern

#### Toast (\~20+ usages)

```ts
// V2
const toast = useToast()
toast({
  title: 'Success',
  description: 'Done',
  status: 'success',
  duration: 3000,
})

// V3
import { toaster } from '@/components/ui/toaster'
toaster.success({
  title: 'Success',
  description: 'Done',
  duration: 3000,
})
```

**Changes:**

- `useToast` hook → `toaster` singleton
- `status` prop → method name

#### Tabs (\~8 usages)

```ts
// V2
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

// V3
<Tabs.Root>
  <Tabs.List>
    <Tabs.Trigger value="one">One</Tabs.Trigger>
    <Tabs.Trigger value="two">Two</Tabs.Trigger>
  </Tabs.List>
  <Tabs.Content value="one">Content 1</Tabs.Content>
  <Tabs.Content value="two">Content 2</Tabs.Content>
</Tabs.Root>
```

### 5.2 Theme Migration

#### Current Theme Structure

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
    ├── react-flow.ts       # Global styles
    └── treeview.ts         # Global styles
```

#### V3 Theme Structure (Target)

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
    └── styles.ts           # Global CSS
```

---

## 6\. Risk Assessment

### 6.1 Technical Risks

| Risk                             | Probability | Impact       | Mitigation                |
| :------------------------------- | :---------- | :----------- | :------------------------ |
| chakra-react-select incompatible | Medium      | High         | Fork or replace           |
| RJSF V6 not ready for Chakra V3  | Medium      | **Critical** | Custom theme or wait      |
| Visual regressions               | High        | Medium       | Visual regression testing |
| Accessibility regressions        | Medium      | High         | Axe testing               |
| Dark mode issues                 | High        | Medium       | Thorough testing          |

### 6.2 Schedule Risks

| Risk                             | Probability | Impact | Mitigation          |
| :------------------------------- | :---------- | :----- | :------------------ |
| Underestimated component changes | Medium      | Medium | Buffer time         |
| Third-party deps block           | Medium      | High   | Early investigation |
| Testing reveals issues           | High        | Medium | Continuous testing  |

---

## 7\. Resource Requirements

### 7.1 Team Allocation

| Role                | Allocation | Duration    |
| :------------------ | :--------- | :---------- |
| Senior Frontend Dev | 100%       | 10-14 weeks |
| Frontend Dev        | 50%        | 10-14 weeks |
| QA Engineer         | 25%        | 8-10 weeks  |
| Designer (review)   | 10%        | 4-6 weeks   |

### 7.2 Tooling

| Tool            | Purpose                   |
| :-------------- | :------------------------ |
| Chakra UI CLI   | Code codemods (partial)   |
| Percy/Chromatic | Visual regression testing |
| Cypress         | Component \+ E2E testing  |
| axe-core        | Accessibility testing     |

---

## 8\. Hybrid Approach Integration

If combining with the UX paradigm migration (adding resource listings):

### 8.1 Parallel Track Option

| Week  | Chakra V3 Track     | Resource UX Track      |
| :---- | :------------------ | :--------------------- |
| 1-2   | Preparation         | OpenAPI enhancement    |
| 3-5   | Foundation \+ Theme | \-                     |
| 6-9   | Component migration | Build resource tables  |
| 10-12 | RJSF migration      | Build resource editors |
| 13-14 | Testing             | Integration testing    |

**Advantage:** Resource components built directly with V3 **Risk:** Larger parallel effort

### 8.2 Sequential Option

1. Complete Chakra V3 migration (10-14 weeks)
2. Then add resource UX (6-9 weeks)

**Advantage:** Lower complexity per phase **Risk:** Longer total timeline (16-23 weeks)

---

## 9\. Recommendations

### 9.1 Pre-Migration Validation (2 weeks)

Before committing to full migration:

1. **Verify RJSF V6 \+ Chakra V3 compatibility**

- This is the highest-risk dependency
- Build proof-of-concept form

2. **Verify chakra-react-select V3 compatibility**

- Test with sample components
- Evaluate alternatives

3. **Create theme migration prototype**

- Convert Button \+ Drawer themes
- Validate dark mode works

### 9.2 Migration Execution

If validation passes:

1. **Use incremental migration**

- Migrate by feature area
- Keep V2/V3 boundary clear

2. **Prioritize shared components first**

- Components used everywhere
- RJSF system

3. **Visual regression testing**

- Set up before migration
- Run on every PR

### 9.3 Fallback Plan

If blockers emerge:

1. **RJSF blocker:** Wait for official support or create custom theme
2. **chakra-react-select blocker:** Replace with alternatives
3. **Timeline overrun:** Pause at natural boundary (e.g., after shared components)

---

## 10\. Summary

### Migration Feasibility: ✅ Feasible with Risks

| Factor                    | Assessment                 |
| :------------------------ | :------------------------- |
| **Technical feasibility** | Yes, with careful planning |
| **Timeline**              | 10-14 weeks realistic      |
| **Effort**                | \~1.5 FTE for 3 months     |
| **Risk level**            | Medium-High                |
| **Critical dependency**   | RJSF \+ Chakra V3 support  |

### Next Steps

1. **Week 1:** Validate RJSF V6 \+ Chakra V3 compatibility
2. **Week 1:** Test chakra-react-select V3 support
3. **Week 2:** Theme migration prototype
4. **Week 2:** Go/No-Go decision

---

**End of Analysis**
