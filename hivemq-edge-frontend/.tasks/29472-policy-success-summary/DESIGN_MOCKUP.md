# Design Mockup - Policy Success Summary

**Task:** 29472 - Policy Success Summary  
**Created:** November 3, 2025

---

## Visual Design Philosophy

As a senior designer, the success summary should:

1. **Provide Confidence** - Users need to feel confident about what they're publishing
2. **Be Scannable** - Quick overview before diving into details
3. **Guide Action** - Clear path to publishing or reviewing details
4. **Reduce Anxiety** - JSON can be intimidating; make it optional and friendly
5. **Match Patterns** - Consistent with error report UX

---

## Layout Structure (Drawer Panel)

```
┌─────────────────────────────────────────────────────────────┐
│ ✕ Report on policy validity                                 │ ← Drawer Header
├─────────────────────────────────────────────────────────────┤
│                                                               │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ✓ SUCCESS ALERT                                         │ │ ← Keep existing
│ │ The policy is fully valid to run on your topology.     │ │
│ │ You can now publish it                                  │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                               │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Policy Details                                          │ │
│ │                                                         │ │
│ │ [🆕 New] Data Policy                         📊         │ │
│ │                                                         │ │
│ │ Policy ID: my-transformation-policy                     │ │
│ │ Topic Filters: 2 filters                                │ │
│ │   • devices/+/temperature                               │ │
│ │   • devices/+/humidity                                  │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                               │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ▼ Schemas (2)                                           │ │ ← Accordion
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ 📄 temperature-schema                               │ │ │
│ │ │    [🆕 New] v1 • JSON                               │ │ │
│ │ │                                                     │ │ │
│ │ │ 📄 humidity-schema                                  │ │ │
│ │ │    [🔄 Update] v2 • JSON                            │ │ │
│ │ └─────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                               │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ▼ Scripts (1)                                           │ │ ← Accordion
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ ⚡ transform-temperature                            │ │ │
│ │ │    [🆕 New] v1 • Transformation                     │ │ │
│ │ └─────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                               │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ▶ View JSON Payload                            [Copy]  │ │ ← Collapsible
│ └─────────────────────────────────────────────────────────┘ │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│                          [Publish] [Close]                   │ ← Footer
└─────────────────────────────────────────────────────────────┘
```

---

## Component Breakdown

### 1. Success Alert (Existing - Enhanced)

**Design:**

```
┌─────────────────────────────────────────────────────────────┐
│ ✓ The policy is fully valid to run on your topology.       │
│   You can now publish it                                    │
└─────────────────────────────────────────────────────────────┘
```

**Chakra Components:**

- `Alert status="success"`
- `AlertIcon` (green checkmark)
- `AlertTitle` and `AlertDescription`

**Colors:**

- Green background (`green.50`)
- Green border (`green.200`)
- Green icon (`green.500`)

**Keep Simple:** This is the reassurance layer - quick glance = all good!

---

### 2. Policy Details Card

**Design:**

```
┌─────────────────────────────────────────────────────────────┐
│ Policy Details                                ← Card Header │
│                                                               │
│ [🆕 New] Data Policy                         📊 ← Badge+Icon│
│                                                               │
│ Policy ID: my-transformation-policy          ← Key Details   │
│ Topic Filters: 2 filters                                     │
│   • devices/+/temperature                                    │
│   • devices/+/humidity                                       │
└─────────────────────────────────────────────────────────────┘
```

**Visual Elements:**

**Status Badge:**

- New: `<Badge colorScheme="blue">New</Badge>` 🆕
- Update: `<Badge colorScheme="orange">Update</Badge>` 🔄

**Policy Icon:**

- Data Policy: Chart/Graph icon (from existing NodeIcon)
- Behavior Policy: Network/Flow icon

**Layout:**

```tsx
<Card>
  <CardHeader>
    <Heading size="sm">Policy Details</Heading>
  </CardHeader>
  <CardBody>
    <HStack justify="space-between">
      <HStack>
        <Badge colorScheme={isNew ? 'blue' : 'orange'}>{isNew ? 'New' : 'Update'}</Badge>
        <Text fontWeight="semibold">Data Policy</Text>
      </HStack>
      <NodeIcon type={policyType} />
    </HStack>

    <VStack align="stretch" mt={4} spacing={2}>
      <Text fontSize="sm" color="gray.600">
        <Text as="span" fontWeight="medium">
          Policy ID:
        </Text>{' '}
        {policyId}
      </Text>

      {/* For Data Policies */}
      <Box>
        <Text fontSize="sm" fontWeight="medium" color="gray.600">
          Topic Filters: {filters.length} filters
        </Text>
        <List spacing={1} ml={4}>
          {filters.map((filter) => (
            <ListItem fontSize="sm" color="gray.600">
              • {filter}
            </ListItem>
          ))}
        </List>
      </Box>
    </VStack>
  </CardBody>
</Card>
```

**Information Hierarchy:**

1. Status (most important) - Badge at top
2. Type - Clear identification
3. ID - Reference for users
4. Key characteristics - What makes this policy unique

---

### 3. Resources Breakdown (Accordion)

**Design Philosophy:**

- Group by type (Schemas, Scripts)
- Show count in header for quick scanning
- Each resource as a clear list item
- Status badges for new vs. update

**Schemas Section:**

```
┌─────────────────────────────────────────────────────────────┐
│ ▼ Schemas (2)                                               │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📄 temperature-schema                                   │ │
│ │    [🆕 New] v1 • JSON                                   │ │
│ │                                                         │ │
│ │ 📄 humidity-schema                                      │ │
│ │    [🔄 Update] v2 • JSON                                │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**Layout:**

```tsx
<Accordion allowMultiple defaultIndex={[0, 1]}>
  {/* Schemas */}
  <AccordionItem>
    <h3>
      <AccordionButton>
        <Box flex="1" textAlign="left" fontWeight="medium">
          Schemas ({schemas.length})
        </Box>
        <AccordionIcon />
      </AccordionButton>
    </h3>
    <AccordionPanel>
      <VStack align="stretch" spacing={3}>
        {schemas.map((schema) => (
          <HStack key={schema.id}>
            <Icon as={LuFileJson} color="purple.500" />
            <VStack align="stretch" spacing={0} flex={1}>
              <Text fontWeight="medium">{schema.id}</Text>
              <HStack fontSize="sm" color="gray.600">
                <Badge size="sm" colorScheme={schema.isNew ? 'blue' : 'orange'}>
                  {schema.isNew ? 'New' : 'Update'}
                </Badge>
                <Text>v{schema.version}</Text>
                <Text>•</Text>
                <Text>{schema.metadata.schemaType}</Text>
              </HStack>
            </VStack>
          </HStack>
        ))}
      </VStack>
    </AccordionPanel>
  </AccordionItem>

  {/* Scripts - Similar structure */}
</Accordion>
```

**Icons:**

- Schema: `LuFileJson` (lucide-react)
- Script: `LuFileCode` or `LuFunction`

**Colors:**

- Schema icon: Purple (`purple.500`)
- Script icon: Orange (`orange.500`)

**Empty State:**

```
┌─────────────────────────────────────────────────────────────┐
│ No additional resources required                             │
│ This policy uses only existing resources                     │
└─────────────────────────────────────────────────────────────┘
```

---

### 4. JSON Payload View (Collapsible)

**Design Philosophy - The Critical Part:**

JSON is intimidating for many users. Our approach:

1. **Hide by Default** - Don't overwhelm
2. **Clear Toggle** - Obvious how to show/hide
3. **Syntax Highlighting** - Make it readable
4. **Tabbed Interface** - Separate policy from resources
5. **Copy Button** - Easy to share/save
6. **Simplified Content** - Remove internal fields

**Collapsed State:**

```
┌─────────────────────────────────────────────────────────────┐
│ ▶ View JSON Payload                            [Copy All]   │
└─────────────────────────────────────────────────────────────┘
```

**Expanded State:**

```
┌─────────────────────────────────────────────────────────────┐
│ ▼ Hide JSON Payload                            [Copy All]   │
│                                                               │
│ ┌────────────────────────────────────────────────────────┐  │
│ │ [Policy] [Schemas (2)] [Scripts (1)]                   │  │ ← Tabs
│ └────────────────────────────────────────────────────────┘  │
│                                                               │
│ ┌────────────────────────────────────────────────────────┐  │
│ │ {                                           [Copy] ←─┐ │  │
│ │   "id": "my-transformation-policy",              ↓ │ │  │
│ │   "matching": {                                    │ │  │
│ │     "topicFilters": [                              │ │  │
│ │       "devices/+/temperature"                  Syntax   │
│ │     ]                                          Highlight│
│ │   },                                               ↓ │  │
│ │   "operations": [...]                              ↓ │  │
│ │ }                                              ←─────┘ │  │
│ └────────────────────────────────────────────────────────┘  │
│                                                               │
│ 📋 Simplified for readability - internal fields omitted      │
└─────────────────────────────────────────────────────────────┘
```

**Implementation:**

```tsx
<Box borderWidth="1px" borderRadius="md">
  <Button
    width="100%"
    onClick={() => setIsExpanded(!isExpanded)}
    rightIcon={isExpanded ? <ChevronUp /> : <ChevronDown />}
    variant="ghost"
    justifyContent="space-between"
  >
    <HStack>
      <Icon as={LuCode2} />
      <Text>{isExpanded ? 'Hide' : 'View'} JSON Payload</Text>
    </HStack>
  </Button>

  <Collapse in={isExpanded}>
    <Box p={4} borderTopWidth="1px">
      {/* Tabs */}
      <Tabs size="sm">
        <TabList>
          <Tab>Policy</Tab>
          <Tab>Schemas ({schemas.length})</Tab>
          <Tab>Scripts ({scripts.length})</Tab>
        </TabList>

        <TabPanels>
          <TabPanel>
            <Box position="relative">
              <Button size="xs" position="absolute" right={2} top={2} onClick={handleCopy} leftIcon={<LuCopy />}>
                Copy
              </Button>

              <Code
                display="block"
                whiteSpace="pre"
                p={4}
                borderRadius="md"
                fontSize="xs"
                maxH="400px"
                overflowY="auto"
              >
                {formatJSON(policyPayload)}
              </Code>
            </Box>
          </TabPanel>
          {/* Similar for other tabs */}
        </TabPanels>
      </Tabs>

      <Text fontSize="xs" color="gray.500" mt={2}>
        📋 Simplified for readability - internal fields omitted
      </Text>
    </Box>
  </Collapse>
</Box>
```

**JSON Simplification Strategy:**

```typescript
function simplifyPayload(payload: object): object {
  // Remove fields that don't help users
  const fieldsToOmit = [
    '__typename',
    'createdAt',
    'lastUpdatedAt',
    'internalId',
    // ... other internal fields
  ]

  // Deep clean and format
  return omitDeep(payload, fieldsToOmit)
}
```

**Alternative: Monaco Editor (More Advanced)**

If we want richer experience:

```tsx
<Editor
  height="400px"
  defaultLanguage="json"
  value={formatJSON(payload)}
  options={{
    readOnly: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    fontSize: 12,
    theme: 'vs-light',
  }}
/>
```

---

## Color Palette

**Status Colors:**

- 🆕 New/Draft: `blue.500` (Blue Badge)
- 🔄 Update/Modified: `orange.500` (Orange Badge)
- ✅ Success: `green.500` (Green Alert)

**Content Colors:**

- Primary text: `gray.800`
- Secondary text: `gray.600`
- Tertiary text: `gray.500`

**Icon Colors:**

- Schema: `purple.500`
- Script: `orange.500`
- Policy: Theme default

**Borders & Backgrounds:**

- Card border: `gray.200`
- Code background: `gray.50`
- Hover state: `gray.100`

---

## Responsive Behavior

**Desktop (> 768px):**

- Drawer width: `500px` (default "sm")
- JSON view: Monaco editor with full features
- All sections visible

**Mobile (< 768px):**

- Full-width drawer
- JSON view: Simple Code component (Monaco might be too heavy)
- Collapsed by default
- Touch-friendly accordion

---

## Accessibility Considerations

**Keyboard Navigation:**

- Tab through all interactive elements
- Enter/Space to expand accordions
- Escape to close drawer
- Focus visible on all elements

**Screen Readers:**

```tsx
// Accordion
<AccordionButton aria-label="Schemas section, 2 items">
  Schemas (2)
</AccordionButton>

// JSON toggle
<Button aria-expanded={isExpanded} aria-controls="json-content">
  {isExpanded ? 'Hide' : 'View'} JSON Payload
</Button>

// Copy button
<Button onClick={handleCopy} aria-label="Copy JSON to clipboard">
  Copy
</Button>
```

**ARIA Live Regions:**

```tsx
// Copy success feedback
<Box role="status" aria-live="polite" aria-atomic="true" position="absolute" left="-10000px">
  {copied && 'JSON copied to clipboard'}
</Box>
```

**Color Contrast:**

- All text meets WCAG AA standards (4.5:1 ratio)
- Status badges have sufficient contrast
- Icons paired with text labels

---

## Motion & Animation

**Smooth Transitions:**

```tsx
// Accordion
;<Collapse in={isOpen} animateOpacity>
  {/* content */}
</Collapse>

// JSON view
transition: 'all 0.2s ease'

// Badge appearance
animation: 'fadeIn 0.3s ease'
```

**Respect Preferences:**

```tsx
// Check for reduced motion
const prefersReducedMotion = useReducedMotion()

<Box
  transition={prefersReducedMotion ? 'none' : 'all 0.2s ease'}
>
```

---

## User Flow

1. **User clicks "Check"** → Validation runs
2. **Validation succeeds** → Drawer opens automatically (existing behavior)
3. **User sees:**
   - ✅ Green success alert (quick reassurance)
   - 📋 Policy details (what am I publishing?)
   - 📦 Resources (what else is being created?)
   - 💻 JSON option (for power users)
4. **User reviews** → Expands sections as needed
5. **User clicks "Publish"** → Confident action!

---

## Edge Cases to Handle

1. **No Resources:**

   - Show "No additional resources required"
   - Don't show empty accordions

2. **Many Resources:**

   - Scrollable list within accordion
   - Consider pagination if > 20 items

3. **Long Policy ID:**

   - Text truncation with tooltip
   - `<Text noOfLines={1} title={fullId}>`

4. **Very Large JSON:**

   - Max height with scroll
   - Consider virtualization for huge payloads

5. **Update Without Changes:**
   - Should this even be possible?
   - Show "No changes detected" message

---

## Implementation Priority

**Must Have (MVP):**

- ✅ Success alert
- ✅ Policy details card
- ✅ Resources breakdown
- ✅ Basic JSON view (Code component)

**Nice to Have:**

- Monaco editor for JSON
- Tabbed JSON view
- JSON simplification
- Copy functionality

**Future Enhancement:**

- Export as PDF
- Share link
- Comparison with existing version

---

## Design System Compliance

Following `.tasks/DESIGN_GUIDELINES.md`:

✅ **Button Variants:**

- Primary action: `<Button variant="primary">Publish</Button>`
- Secondary: `<Button variant="ghost">Close</Button>`

✅ **Modal/Drawer Icons:**

- Informational (blue) for helpful content
- Success (green) for validation passed

✅ **Consistent Spacing:**

- VStack/HStack with defined spacing
- Card padding follows theme

✅ **Typography:**

- Heading hierarchy: h2 → h3 → h4
- Font sizes from theme

---

## Success Metrics (UX)

**How do we know this design works?**

1. **Users feel confident** - Reduced support tickets about "what gets published?"
2. **Users understand** - Clear distinction between new and update
3. **Users aren't overwhelmed** - JSON hidden by default, progressive disclosure
4. **Users can act** - Path to publish is clear
5. **Power users satisfied** - JSON view available for inspection

---

## Visual Refinements

**Subtle Enhancements:**

1. **Badges with Icons:**

```tsx
<Badge colorScheme="blue">
  <HStack spacing={1}>
    <Icon as={LuPlus} boxSize="10px" />
    <Text>New</Text>
  </HStack>
</Badge>
```

2. **Resource Icons with Background:**

```tsx
<Circle size="32px" bg="purple.50">
  <Icon as={LuFileJson} color="purple.500" />
</Circle>
```

3. **Dividers for Clarity:**

```tsx
<Divider my={3} />
```

4. **Shadow on Cards:**

```tsx
<Card shadow="sm">
```

---

This design balances **information density** with **usability**, providing power users with details while keeping casual users from feeling overwhelmed. The progressive disclosure pattern (collapsed accordions, hidden JSON) respects cognitive load while maintaining accessibility.
