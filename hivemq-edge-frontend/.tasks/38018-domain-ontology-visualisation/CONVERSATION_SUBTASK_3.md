# Task 38018 - Conversation: Subtask 3 - Network Graph Details Panel

**Date:** November 4, 2025  
**Subtask:** Details Panel for Network Graph View  
**Status:** ✅ COMPLETE

---

## Implementation Summary

Successfully implemented a bottom slide-in details panel with **container/content separation** for easy layout swapping.

### Files Created

1. **`NetworkGraphDetailsPanelContainer.tsx`** - Layout/positioning component
2. **`NetworkGraphDetailsPanel.tsx`** - Content display component

### Files Updated

1. **`NetworkGraphView.tsx`** - Added selection state and panel integration

### Key Features Delivered

- ✅ Node click opens details panel with smooth slide-in animation
- ✅ Displays node type, label, connection stats
- ✅ Lists all connected nodes with edge types and direction
- ✅ Close button clears selection
- ✅ Action buttons (View Config, Filter) with placeholder handlers
- ✅ No overlay conflicts - panel is within graph container
- ✅ Container/content separation allows easy layout swapping

### Architecture Benefits

The **container/content separation** means if you encounter UX issues with the bottom slide-in, you can easily swap to:

- Side panel (shrinks graph width)
- Modal (centered overlay)
- Popover (floating near node)
- Inline card (below graph)

Just replace `NetworkGraphDetailsPanelContainer` - content component stays the same!

---

## Objective

Add an interactive details panel that displays node information when clicking nodes in the Network Graph View, following UX best practices for overlay/panel patterns in the existing codebase.

---

## Design Considerations

### UX Challenge: Nested Panels

**Current Context:**

- Network Graph View lives in `EdgePropertyDrawer` (already a side panel)
- `EdgePropertyDrawer` uses `ExpandableDrawer` from Chakra UI
- Drawer is already overlaying the Workspace canvas
- Opening another drawer/panel inside this creates **layering complexity**

### Existing Patterns in Codebase

**Pattern 1: Drawers (Side Panels)**

- Used in: `EdgePropertyDrawer`, `DevicePropertyDrawer`, `NodePropertyDrawer`
- Component: `ExpandableDrawer` (wraps Chakra `Drawer`)
- Placement: `right`, sizes: `lg` or `full` (expandable)
- Has overlay backdrop
- **Issue:** Nesting drawers is problematic (overlay conflicts)

**Pattern 2: Modals**

- Used in: Various confirmation dialogs, forms
- Component: Chakra `Modal`
- Centered overlay
- **Issue:** Modal breaks visual flow, forces focus away from graph

**Pattern 3: Inline Panels**

- Used in: Various data tables, forms
- No component wrapper needed
- **Advantage:** No overlay conflicts, stays in context

---

## Recommended Solution: **Inline Slide-In Panel**

### Design Pattern: Bottom Slide-In Panel

**Why this approach:**

1. ✅ **No overlay conflicts** - Panel slides up from bottom of the graph viewport
2. ✅ **Maintains context** - Graph stays visible, node selection clear
3. ✅ **Familiar pattern** - Similar to mobile bottom sheets
4. ✅ **Non-blocking** - User can still pan/zoom graph with panel open
5. ✅ **Responsive** - Can adjust height based on content

### Visual Mockup

```
┌────────────────────────────────────────────────────────┐
│ Edge Property Drawer                              [×] ▢ │
├────────────────────────────────────────────────────────┤
│ [Tabs: Cluster | Wheel | Chord | Sankey | Network]    │
├────────────────────────────────────────────────────────┤
│                                                        │
│   Network Graph View (600px height)                   │
│                                                        │
│     ○─────○                                           │
│     │     ↓                                           │
│     ○  ←  ● [SELECTED NODE - tag-truc1]              │
│     │                                                  │
│     ○─────○                                           │
│                                                        │
│ ┌──────────────────────────────────────────────────┐  │
│ │ 📊 Node Details                             [×]  │  │ ← Slide-in panel
│ ├──────────────────────────────────────────────────┤  │
│ │ Type: TAG                                        │  │
│ │ Label: tag-truc1                                 │  │
│ │ Connections: 1 outgoing                          │  │
│ │                                                  │  │
│ │ Connected To:                                    │  │
│ │ • topic/mock/test1 (Northbound)                 │  │
│ │                                                  │  │
│ │ [View Configuration →]  [Filter Graph]          │  │
│ └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### Implementation Details

**Component Structure:**

```tsx
<NetworkGraphView>
  <Box position="relative" h="600px">
    <ReactFlow .../>

    {/* Slide-in panel */}
    <Collapse in={selectedNodeId !== null}>
      <Box
        position="absolute"
        bottom={0}
        left={0}
        right={0}
        bg="white"
        borderTop="2px solid"
        borderColor="gray.200"
        p={4}
        maxH="300px"
        overflow="auto"
        boxShadow="lg"
        zIndex={10}
      >
        <NetworkGraphDetailsPanel
          node={selectedNode}
          onClose={clearSelection}
        />
      </Box>
    </Collapse>
  </Box>
</NetworkGraphView>
```

**Key Features:**

- `Collapse` from Chakra provides smooth slide animation
- `position="absolute"` keeps it within graph viewport
- `bottom={0}` slides from bottom
- `maxH="300px"` limits height to 50% of graph
- `zIndex={10}` ensures it's above graph controls
- `overflow="auto"` scrolls if content is long

---

## Alternative Approaches Considered

### ❌ Alternative 1: Nested Drawer

```tsx
<Drawer>
  {' '}
  {/* EdgePropertyDrawer */}
  <DomainOntologyManager>
    <NetworkGraphView>
      <Drawer>
        {' '}
        {/* Details drawer - nested! */}
        <NodeDetails />
      </Drawer>
    </NetworkGraphView>
  </DomainOntologyManager>
</Drawer>
```

**Problems:**

- Two overlays competing
- Confusing z-index management
- Close behaviors conflict (clicking overlay closes which one?)
- Not recommended by Chakra UI

### ❌ Alternative 2: Modal

```tsx
<Modal isOpen={!!selectedNode}>
  <NodeDetails />
</Modal>
```

**Problems:**

- Breaks visual flow
- Loses graph context
- User can't see what node they selected
- Feels heavyweight for simple info display

### ❌ Alternative 3: Popover

```tsx
<Popover>
  <PopoverTrigger>
    <Node />
  </PopoverTrigger>
  <PopoverContent>
    <NodeDetails />
  </PopoverContent>
</Popover>
```

**Problems:**

- Doesn't work with React Flow nodes (they're in canvas)
- Positioning is complex with zoom/pan
- Limited space for details

### ✅ Alternative 4: Side Panel (within graph container)

```tsx
<Flex>
  <Box flex={1}>
    <ReactFlow />
  </Box>
  {selectedNode && (
    <Box w="300px" borderLeft="1px">
      <NodeDetails />
    </Box>
  )}
</Flex>
```

**Pros:**

- Clean separation
- No overlay issues
- Scrollable

**Cons:**

- Shrinks graph width when open
- May feel cramped on smaller screens

**Decision:** Use bottom slide-in as primary, side panel as future alternative

---

## Implementation Plan

### Phase 1: Basic Panel Structure

**Files to create:**

1. `NetworkGraphDetailsPanel.tsx` - Details panel component
2. `useNetworkGraphSelection.ts` - Selection state hook (optional)

**Files to update:**

1. `NetworkGraphView.tsx` - Add panel, handle selection

### Phase 2: Panel Content

**Features:**

- Node type badge (TAG/TOPIC/FILTER with color)
- Node label
- Connection count
- List of connected nodes with edge types
- Action buttons (View Config, Filter, Close)

### Phase 3: Interactions

**Features:**

- Click node → open panel with animation
- Click panel close → close panel, clear selection
- Click "View Configuration" → navigate to adapter/bridge config
- Click "Filter" → highlight related nodes

---

## Technical Specifications

### State Management

**Local state in NetworkGraphView:**

```tsx
const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)

const onNodeClick = useCallback((_event: React.MouseEvent, node: NetworkGraphNode) => {
  setSelectedNodeId(node.id)
}, [])

const clearSelection = useCallback(() => {
  setSelectedNodeId(null)
}, [])
```

### Panel Component Props

```tsx
interface NetworkGraphDetailsPanelProps {
  node: NetworkGraphNode | null
  onClose: () => void
  onNavigateToConfig?: (nodeId: string) => void
  onFilterByNode?: (nodeId: string) => void
}
```

### Panel Content Structure

```tsx
<VStack align="stretch" spacing={4}>
  <HStack justify="space-between">
    <Heading size="sm">Node Details</Heading>
    <IconButton icon={<CloseIcon />} onClick={onClose} />
  </HStack>

  <Box>
    <Badge colorScheme={getNodeColor(node.data.type)}>{node.data.type}</Badge>
    <Text fontSize="lg" fontWeight="bold">
      {node.data.label}
    </Text>
  </Box>

  <Box>
    <Text fontSize="sm" color="gray.600">
      Connections
    </Text>
    <Text>{node.data.connectionCount} connections</Text>
  </Box>

  {/* Connected nodes list */}
  <VStack align="stretch">
    {connectedNodes.map((connection) => (
      <HStack key={connection.id}>
        <Badge>{connection.edgeType}</Badge>
        <Text>{connection.label}</Text>
      </HStack>
    ))}
  </VStack>

  <ButtonGroup>
    <Button onClick={() => onNavigateToConfig(node.id)}>View Configuration</Button>
    <Button onClick={() => onFilterByNode(node.id)}>Filter Graph</Button>
  </ButtonGroup>
</VStack>
```

---

## Accessibility Considerations

1. **Keyboard Navigation**

   - Panel focusable when opens
   - Escape key closes panel
   - Tab navigation within panel

2. **Screen Readers**

   - Announce when panel opens
   - Proper ARIA labels
   - Semantic HTML structure

3. **Focus Management**
   - Auto-focus panel when opens
   - Return focus to node (if possible) when closes

---

## Next Steps

1. Create `NetworkGraphDetailsPanel.tsx` with slide-in layout
2. Update `NetworkGraphView.tsx` to handle node selection
3. Add panel content (metadata, connections)
4. Add action buttons (navigation, filtering)
5. Test accessibility
6. Add component test

---

**Status:** 🚧 READY TO IMPLEMENT  
**Estimated Time:** 2-3 hours  
**Pattern:** Bottom slide-in panel (no overlay conflicts)
