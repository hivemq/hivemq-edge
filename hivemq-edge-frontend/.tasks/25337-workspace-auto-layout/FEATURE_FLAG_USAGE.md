# Feature Flag Usage Guide

## Configuration Location

The `WORKSPACE_AUTO_LAYOUT` feature flag is now integrated into the main config structure at:
**File:** `src/config/index.ts`

```typescript
import config from '@/config'
// Check if auto-layout is enabled
if (config.features.WORKSPACE_AUTO_LAYOUT) {
  // Show layout controls
}
```

## Environment Variable

Set in `.env.local`:

```bash
VITE_FEATURE_AUTO_LAYOUT=true
```

## Usage Examples

### Example 1: Conditional Component Rendering

```typescript
import config from '@/config'
import { LayoutControls } from './LayoutControls'
function WorkspaceToolbar() {
  return (
    <HStack>
      {/* ...other controls... */}
      {config.features.WORKSPACE_AUTO_LAYOUT && (
        <LayoutControls />
      )}
    </HStack>
  )
}
```

### Example 2: Conditional Hook Usage

```typescript
import config from '@/config'
import { useLayoutEngine } from '@/modules/Workspace/hooks/useLayoutEngine'
function MyComponent() {
  const layoutEngine = config.features.WORKSPACE_AUTO_LAYOUT ? useLayoutEngine() : null
  const handleLayout = () => {
    if (layoutEngine) {
      layoutEngine.applyLayout()
    }
  }
  // ...
}
```

### Example 3: Feature Detection

```typescript
import config from '@/config'
export const isAutoLayoutAvailable = (): boolean => {
  return config.features.WORKSPACE_AUTO_LAYOUT
}
// Usage
if (isAutoLayoutAvailable()) {
  console.log('Auto-layout feature is enabled')
}
```

## Benefits of Central Config

✅ **Single Source of Truth** - All feature flags in one place  
✅ **Consistent API** - Same pattern as other features  
✅ **Type-Safe** - TypeScript interface ensures correctness  
✅ **Easy Discovery** - All flags visible in `config.features`  
✅ **No Duplication** - Removed separate `features.ts` file

## Migration from Old Structure

**Before (separate features.ts):**

```typescript
import { FEATURES } from '@/config/features'
if (FEATURES.WORKSPACE_AUTO_LAYOUT) { ... }
```

**After (integrated config):**

```typescript
import config from '@/config'
if (config.features.WORKSPACE_AUTO_LAYOUT) { ... }
```

## All Available Feature Flags

```typescript
config.features = {
  DEV_MOCK_SERVER: boolean           // DEV-only mock server
  DATAHUB_FSM_REACT_FLOW: boolean    // React Flow for FSM
  WORKSPACE_EXPERIMENTAL: boolean     // Experimental workspace features
  WORKSPACE_AUTO_LAYOUT: boolean      // Auto-layout algorithms (NEW!)
}
```

---

**Updated:** October 27, 2025  
**Status:** ✅ Integrated into main config
