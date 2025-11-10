# SUBTASK 7: Complete Adapter Wizard Flow

**Date:** November 10, 2025  
**Status:** 🔄 In Progress  
**Priority:** High - Final piece for Phase 1

---

## Overview

Complete the adapter wizard by implementing the final step: actually creating the adapter via API call, replacing ghost nodes with real nodes, and cleaning up wizard state.

---

## Current State

**What Works:**

- ✅ Wizard trigger button
- ✅ Progress bar with navigation
- ✅ Ghost nodes with proper positioning
- ✅ Protocol selection (Step 2)
- ✅ Configuration form (Step 3)
- ✅ Workspace restrictions during wizard
- ✅ Data saved to wizard store

**What's Missing:**

- ❌ API call to create adapter
- ❌ Ghost → Real node transition
- ❌ Success/error feedback
- ❌ Wizard cleanup
- ❌ Complete button functionality

---

## Implementation Plan

### 1. Update WizardAdapterForm to Handle Submit

**Current:** Form submission saves to wizard store  
**Needed:** Form submission should trigger wizard completion

**File:** `WizardAdapterForm.tsx`

```typescript
const WizardAdapterForm: FC<WizardAdapterFormProps> = ({
  protocolId,
  onSubmit, // Currently just saves to store
  onBack,
}) => {
  // Change behavior: onSubmit should mark as ready, not complete wizard
  const handleFormSubmit = (data: Adapter) => {
    onSubmit(data) // Save to store
    // The "Submit" button in footer will trigger completion
  }
}
```

### 2. Create Wizard Completion Handler

**New File:** `useCompleteAdapterWizard.ts`

```typescript
export const useCompleteAdapterWizard = () => {
  const { configurationData } = useWizardConfiguration()
  const { actions } = useWizardStore.getState()
  const { mutateAsync: createAdapter } = useCreateProtocolAdapter()
  const { getNodes, setNodes, getEdges, setEdges } = useReactFlow()
  const toast = useToast()

  const completeWizard = async () => {
    try {
      const { protocolId, adapterConfig } = configurationData

      // 1. Create adapter via API
      const newAdapter = await createAdapter({
        ...adapterConfig,
        type: protocolId,
      })

      // 2. Remove ghost nodes and edges
      const nodes = getNodes()
      const edges = getEdges()
      const realNodes = removeGhostNodes(nodes)
      const realEdges = removeGhostEdges(edges)

      // 3. Real nodes will be added by useGetFlowElements hook automatically
      // Just clean up ghosts
      setNodes(realNodes)
      setEdges(realEdges)

      // 4. Complete wizard
      actions.completeWizard()

      // 5. Show success
      toast({
        title: 'Adapter created',
        description: `${newAdapter.id} has been created successfully`,
        status: 'success',
      })
    } catch (error) {
      // Show error
      actions.setError(error.message)
      toast({
        title: 'Error creating adapter',
        description: error.message,
        status: 'error',
      })
    }
  }

  return { completeWizard }
}
```

### 3. Update WizardProgressBar to Handle Complete

**File:** `WizardProgressBar.tsx`

```typescript
const WizardProgressBar: FC = () => {
  // ...existing code
  const { completeWizard } = useCompleteAdapterWizard()

  const handleNext = () => {
    if (isLastStep) {
      // Complete wizard with API call
      completeWizard()
    } else {
      nextStep()
    }
  }

  return (
    // ...
    <Button
      onClick={handleNext}
      isLoading={isCompleting}  // Show loading state
    >
      {isLastStep ? 'Complete' : 'Next'}
    </Button>
  )
}
```

### 4. Alternative Approach: WizardAdapterConfiguration

Instead of in progress bar, handle completion in the configuration component:

**File:** `WizardAdapterConfiguration.tsx`

```typescript
const WizardAdapterConfiguration: FC = () => {
  const { currentStep } = useWizardState()
  const { completeWizard } = useCompleteAdapterWizard()

  // Step 2: Configuration Form
  if (currentStep === 2) {
    const handleFormSubmit = async (adapterData: Adapter) => {
      // Save to store
      updateConfiguration({
        protocolId: selectedProtocolId,
        adapterConfig: adapterData,
      })

      // Trigger completion
      await completeWizard()
    }

    return (
      <WizardAdapterForm
        onSubmit={handleFormSubmit}
      />
    )
  }
}
```

---

## API Integration

### useCreateProtocolAdapter Hook

**Already exists:** `src/api/hooks/useProtocolAdapters/useCreateProtocolAdapter.ts`

```typescript
export const useCreateProtocolAdapter = () => {
  return useMutation({
    mutationFn: (adapter: Adapter) => {
      return ApiClient.adapters.createAdapter(adapter)
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['adapters'])
    },
  })
}
```

### Usage:

```typescript
const { mutateAsync, isLoading } = useCreateProtocolAdapter()

try {
  const newAdapter = await mutateAsync({
    id: 'my-adapter',
    type: 'modbus-tcp',
    config: { ... }
  })
  // Success!
} catch (error) {
  // Handle error
}
```

---

## Ghost → Real Transition

### Challenge

Ghost nodes are at the correct position, but when `useGetFlowElements` refreshes and adds real nodes, we need to ensure they appear at the same position.

### Solution 1: Let useGetFlowElements Handle It (Recommended)

```typescript
// After creating adapter via API:
// 1. Remove ghost nodes
setNodes(removeGhostNodes(getNodes()))
setEdges(removeGhostEdges(getEdges()))

// 2. Invalidate queries (already done in mutation)
// This triggers useGetFlowElements to refresh

// 3. useGetFlowElements will create real nodes at correct position
// (uses same positioning algorithm)
```

**Pros:**

- Consistent with existing flow
- No manual node creation
- Positioning already correct

**Cons:**

- Small delay while query refreshes
- Might see a flicker

### Solution 2: Create Real Nodes Immediately

```typescript
// After creating adapter via API:
const nbAdapters = adapters.length - 1 // Before new one was added
const edgeNode = getNodes().find((n) => n.id === IdStubs.EDGE_NODE)
const theme = useTheme()

// Create real nodes
const { nodeAdapter, nodeDevice, edgeConnector, deviceConnector } = createAdapterNode(
  type,
  newAdapter,
  nbAdapters,
  nbAdapters + 1,
  theme
)

// Replace ghost with real
const nodes = getNodes().filter((n) => !n.data?.isGhost)
const edges = getEdges().filter((e) => !e.id?.startsWith('ghost-'))

setNodes([...nodes, nodeAdapter, nodeDevice])
setEdges([...edges, edgeConnector, deviceConnector])
```

**Pros:**

- Immediate visual feedback
- No flicker
- Smooth transition

**Cons:**

- Duplicates node creation logic
- Need to sync with useGetFlowElements

**Decision:** Use **Solution 1** - simpler and more maintainable

---

## Error Handling

### Validation Errors

```typescript
try {
  await createAdapter(adapterConfig)
} catch (error) {
  if (error.status === 400) {
    // Validation error
    actions.setError(error.body.detail)
  } else if (error.status === 409) {
    // Conflict (duplicate ID)
    actions.setError('An adapter with this ID already exists')
  } else {
    // Generic error
    actions.setError('Failed to create adapter')
  }
}
```

### Show Error in Wizard

```typescript
const { errorMessage } = useWizardState()

{errorMessage && (
  <Alert status="error">
    <AlertIcon />
    {errorMessage}
  </Alert>
)}
```

---

## Success Feedback

### Toast Notification

```typescript
toast({
  title: 'Adapter created',
  description: `${newAdapter.id} has been created successfully`,
  status: 'success',
  duration: 5000,
  isClosable: true,
})
```

### Visual Feedback

```typescript
// Optional: Brief highlight on new node
const newNodeId = `ADAPTER_NODE@${newAdapter.id}`
setTimeout(() => {
  const node = getNodes().find((n) => n.id === newNodeId)
  if (node) {
    // Briefly highlight the node
    node.style = {
      ...node.style,
      boxShadow: '0 0 0 4px rgba(72, 187, 120, 0.6)',
    }
    setNodes([...getNodes()])

    // Remove highlight after 2 seconds
    setTimeout(() => {
      node.style = { ...node.style, boxShadow: undefined }
      setNodes([...getNodes()])
    }, 2000)
  }
}, 100)
```

---

## Wizard Cleanup

### completeWizard Action

Already exists in store:

```typescript
completeWizard: () => {
  set({
    isActive: false,
    entityType: null,
    currentStep: 0,
    totalSteps: 0,
    selectedNodeIds: [],
    selectionConstraints: null,
    ghostNodes: [],
    ghostEdges: [],
    configurationData: {},
    isConfigurationValid: false,
    isSidePanelOpen: false,
    errorMessage: null,
  })
}
```

---

## Files to Create/Modify

### Create:

1. **`useCompleteAdapterWizard.ts`** - Hook to handle wizard completion
   - API call
   - Ghost cleanup
   - Success/error handling

### Modify:

1. **`WizardAdapterConfiguration.tsx`** - Trigger completion on form submit
2. **`WizardProgressBar.tsx`** - Handle loading state during completion
3. **`WizardAdapterForm.tsx`** - Update submit handler (optional)

---

## Testing Checklist

- [ ] Click Complete button
- [ ] Loading indicator shows
- [ ] API call succeeds
- [ ] Ghost nodes disappear
- [ ] Real nodes appear at same position
- [ ] Success toast shows
- [ ] Wizard closes
- [ ] Progress bar disappears
- [ ] Workspace unlocked
- [ ] New adapter visible and functional
- [ ] Error handling works (try duplicate ID)
- [ ] Cancel during loading works

---

## Implementation Order

1. ✅ Create `useCompleteAdapterWizard` hook
2. ✅ Update `WizardAdapterConfiguration` to use it
3. ✅ Add loading state to progress bar
4. ✅ Test success flow
5. ✅ Add error handling
6. ✅ Test error flow
7. ✅ Add success feedback (toast)
8. ✅ Polish and cleanup

---

**Status:** Ready to implement
