# Workspace Wizard - Workflow & DOM Element Reference

**Purpose:** Bridge between wizard implementation and E2E testing  
**Last Updated:** November 13, 2025  
**Status:** 🟢 Active Development

---

## Document Purpose

This document provides:

1. **Complete wizard workflows** with step-by-step flow
2. **DOM element mapping** (data-testid, aria-label, roles)
3. **Page Object design guidance**
4. **Critical test scenarios** and test paths
5. **Bridge between code and tests**

---

## Wizard Architecture Overview

### Three Core Components

1. **CreateEntityButton** - Trigger
2. **WizardProgressBar** - Navigation & Status
3. **Ghost Nodes** - Visual Preview
4. **Configuration Panels** - Form Input

### Wizard State Flow

```
[INACTIVE]
    ↓ (Click Create Button + Select Type)
[STEP 0: Preview/Selection]
    ↓ (Click Next)
[STEP 1: Configuration]
    ↓ (Click Next/Complete)
[COMPLETE] → [INACTIVE]
```

---

## Component 1: CreateEntityButton (Trigger)

### Purpose

Dropdown button in CanvasToolbar that initiates wizard

### DOM Elements

| Element                   | data-testid            | aria-label                                 | Role       | Selector                                |
| ------------------------- | ---------------------- | ------------------------------------------ | ---------- | --------------------------------------- |
| **Menu Button**           | `create-entity-button` | `workspace.wizard.trigger.buttonAriaLabel` | `button`   | `[data-testid="create-entity-button"]`  |
| **Menu List**             | -                      | `workspace.wizard.trigger.menuTitle`       | `menu`     | `[role="menu"]`                         |
| **Entity Menu Item**      | `wizard-option-{TYPE}` | -                                          | `menuitem` | `[data-testid="wizard-option-ADAPTER"]` |
| **Integration Menu Item** | `wizard-option-{TYPE}` | -                                          | `menuitem` | `[data-testid="wizard-option-TAG"]`     |

**Available Types:**

- **Entities:** `ADAPTER`, `BRIDGE`, `COMBINER`, `ASSET_MAPPER`, `GROUP`
- **Integration Points:** `TAG`, `TOPIC_FILTER`, `DATA_MAPPING_NORTH`, `DATA_MAPPING_SOUTH`, `DATA_COMBINING`

### States

| State        | Condition   | Visual                        |
| ------------ | ----------- | ----------------------------- |
| **Enabled**  | `!isActive` | Normal button                 |
| **Disabled** | `isActive`  | Grayed out, has title tooltip |
| **Open**     | Menu open   | Dropdown visible              |

### Page Object Methods

```typescript
wizardPage.createEntityButton // Get button
wizardPage.createEntityButton.click() // Open menu
wizardPage.wizardMenu.selectOption('ADAPTER') // Click menu item
```

### Test Scenarios

1. **Accessibility:** Button has aria-label, menu has role="menu"
2. **Disabled State:** Button disabled when wizard active
3. **Menu Content:** All entity types visible and clickable
4. **Integration Points:** Separate section after divider
5. **Capability Check:** Asset Mapper disabled without Pulse

---

## Component 2: WizardProgressBar

### Purpose

Shows wizard progress, provides navigation, displays constraints

### DOM Elements

| Element             | data-testid              | aria-label                                    | Role          | Selector                                 |
| ------------------- | ------------------------ | --------------------------------------------- | ------------- | ---------------------------------------- |
| **Container**       | `wizard-progress-bar`    | `workspace.wizard.progress.ariaLabel`         | `region`      | `[data-testid="wizard-progress-bar"]`    |
| **Progress Bar**    | -                        | `workspace.wizard.progress.progressAriaLabel` | `progressbar` | `[role="progressbar"]`                   |
| **Back Button**     | `wizard-back-button`     | `workspace.wizard.progress.backLabel`         | `button`      | `[data-testid="wizard-back-button"]`     |
| **Next Button**     | `wizard-next-button`     | `workspace.wizard.progress.nextLabel`         | `button`      | `[data-testid="wizard-next-button"]`     |
| **Complete Button** | `wizard-complete-button` | `workspace.wizard.progress.completeLabel`     | `button`      | `[data-testid="wizard-complete-button"]` |
| **Cancel Button**   | `wizard-cancel-button`   | `workspace.wizard.progress.cancelAriaLabel`   | `button`      | `[data-testid="wizard-cancel-button"]`   |

### Content Elements

| Content              | Location           | Format                     |
| -------------------- | ------------------ | -------------------------- |
| **Step Label**       | Top left           | "Step X of Y"              |
| **Progress %**       | Progress bar value | 0-100%                     |
| **Step Description** | Bottom text        | Translated description key |

### States

| State             | Condition                        | Buttons Visible        |
| ----------------- | -------------------------------- | ---------------------- |
| **First Step**    | `currentStep === 0`              | Next, Cancel           |
| **Middle Step**   | `0 < currentStep < totalSteps-1` | Back, Next, Cancel     |
| **Last Step**     | `currentStep === totalSteps-1`   | Back, Complete, Cancel |
| **Next Disabled** | Selection constraints not met    | Next disabled          |

### Page Object Methods

```typescript
wizardPage.progressBar.container // Get container
wizardPage.progressBar.container.should('contain', 'Step 1') // Check step
wizardPage.progressBar.nextButton.click() // Click next
wizardPage.progressBar.backButton.click() // Go back
wizardPage.progressBar.cancelButton.click() // Cancel wizard
```

### Test Scenarios

1. **Accessibility:** Region has aria-label, progress has percentage
2. **Step Progression:** Label updates correctly (Step 1 of 3)
3. **Progress Bar:** Visual progress increases (33%, 66%, 100%)
4. **Button States:** Back hidden on first step, Next becomes Complete
5. **Constraints:** Next disabled when selection invalid
6. **Cancel:** Closes wizard, removes ghosts, resets state

---

## Component 3: Ghost Nodes & Edges

### Purpose

Visual preview of what will be created before configuration

### DOM Elements

| Element        | Selector                   | Attributes                   |
| -------------- | -------------------------- | ---------------------------- |
| **Ghost Node** | `[data-ghost="true"]`      | Lower opacity, dashed border |
| **Ghost Edge** | `[data-ghost-edge="true"]` | Dashed line style            |

### States

| State         | Visual                        | Behavior              |
| ------------- | ----------------------------- | --------------------- |
| **Preview**   | Semi-transparent              | Not clickable         |
| **Connected** | Shows edges to existing nodes | Demonstrates topology |

### Page Object Methods

```typescript
wizardPage.canvas.ghostNode // Get ghost node(s)
wizardPage.canvas.ghostNode.should('be.visible') // Check visible
wizardPage.canvas.ghostEdges // Get ghost edge(s)
wizardPage.canvas.ghostEdges.should('exist') // Check exist
```

### Test Scenarios

1. **Visibility:** Ghost nodes appear after selecting wizard type
2. **Positioning:** Ghosts positioned logically on canvas
3. **Connections:** Ghost edges show correct topology
4. **Removal:** Ghosts removed on cancel
5. **Replacement:** Ghosts replaced by real nodes on completion

---

## Workflow: ADAPTER Wizard (3 Steps)

### Step 0: Ghost Preview

**Purpose:** Show adapter topology preview (DEVICE → ADAPTER → EDGE)

**DOM State:**

- Progress bar: "Step 1 of 3"
- Progress: 33%
- Ghost nodes: 3 (Device, Adapter, Edge)
- Ghost edges: 2 connections
- Next button: Enabled (no selection required)

**Page Object:**

```typescript
wizardPage.progressBar.container.should('contain', 'Step 1')
wizardPage.canvas.ghostNode.should('be.visible')
wizardPage.canvas.ghostEdges.should('have.length', 2)
wizardPage.progressBar.nextButton.should('not.be.disabled')
```

**Test Path:**

1. ✅ Progress bar visible with "Step 1 of 3"
2. ✅ 3 ghost nodes on canvas
3. ✅ 2 ghost edges connecting nodes
4. ✅ Next button enabled

---

### Step 1: Protocol Selection

**Purpose:** Select adapter protocol type (HTTP, OPC-UA, MQTT, etc.)

**DOM State:**

- Progress bar: "Step 2 of 3"
- Progress: 66%
- Configuration panel: Protocol selector visible
- Data loaded: `/api/v1/management/protocol-adapters/types`

**DOM Elements:**

| Element               | data-testid / selector                                                         | Type            |
| --------------------- | ------------------------------------------------------------------------------ | --------------- |
| **Protocol Selector** | `[data-testid="adapter-protocol-selector"], [data-testid="protocol-selector"]` | Dropdown/Select |
| **Protocol Options**  | Text contains protocol name                                                    | MenuItems       |

**Page Object:**

```typescript
wizardPage.progressBar.container.should('contain', 'Step 2')
cy.wait('@getProtocols')
wizardPage.adapterConfig.protocolSelector.should('be.visible')
wizardPage.adapterConfig.selectProtocol('HTTP')
```

**Test Path:**

1. ✅ Progress bar shows "Step 2 of 3"
2. ✅ API call for protocol types
3. ✅ Protocol selector visible
4. ✅ Can select protocol
5. ✅ Next button enabled after selection

---

### Step 2: Adapter Configuration

**Purpose:** Configure protocol-specific settings and adapter name

**DOM State:**

- Progress bar: "Step 3 of 3"
- Progress: 100%
- Configuration form: Protocol-specific fields
- Next button: Becomes "Complete"

**DOM Elements:**

| Element             | data-testid / selector                                 | Type       |
| ------------------- | ------------------------------------------------------ | ---------- |
| **Config Form**     | `[data-testid="adapter-config-form"], form`            | Form       |
| **Adapter Name**    | `[data-testid="adapter-name-input"], input[name="id"]` | Text Input |
| **Protocol Fields** | Various (protocol-specific)                            | Mixed      |
| **Submit Button**   | `button[type="submit"]`                                | Button     |

**Page Object:**

```typescript
wizardPage.progressBar.container.should('contain', 'Step 3')
wizardPage.adapterConfig.configForm.should('be.visible')
wizardPage.adapterConfig.setAdapterName('My Adapter')
wizardPage.adapterConfig.configForm.within(() => {
  cy.get('button[type="submit"]').click()
})
```

**Test Path:**

1. ✅ Progress bar shows "Step 3 of 3"
2. ✅ Complete button visible (not "Next")
3. ✅ Form has protocol-specific fields
4. ✅ Can fill adapter name
5. ✅ Can submit form
6. ✅ API POST to create adapter
7. ✅ Success state shown

---

## Workflow: COMBINER Wizard (2 Steps)

### Step 0: Select Data Sources

**Purpose:** Select 2+ sources (adapters/bridges with COMBINE capability)

**DOM State:**

- Progress bar: "Step 1 of 2"
- Progress: 50%
- Selection panel: Constraint message visible
- Canvas: Allowed nodes selectable, others disabled
- Next button: Disabled until minNodes met

**DOM Elements:**

| Element                | data-testid / selector                   | Type            |
| ---------------------- | ---------------------------------------- | --------------- |
| **Selection Panel**    | `[data-testid="wizard-selection-panel"]` | Panel           |
| **Selected Count**     | `[data-testid="selection-count"]`        | Text            |
| **Selected List**      | `[data-testid="selected-nodes-list"]`    | List            |
| **Validation Message** | `[data-testid="selection-validation"]`   | Text            |
| **Canvas Nodes**       | `[data-id="{nodeId}"]`                   | Canvas Elements |
| **Disabled Nodes**     | `[data-disabled="true"]`                 | Canvas Elements |

**Selection Constraints:**

- **minNodes:** 2
- **allowedNodeTypes:** `ADAPTER_NODE` (with COMBINE capability), `BRIDGE_NODE`
- **excludeGrouped:** false

**Page Object:**

```typescript
wizardPage.progressBar.container.should('contain', 'Step 1')
wizardPage.selectionPanel.panel.should('be.visible')
wizardPage.selectionPanel.selectedCount.should('contain', '0 selected')
wizardPage.selectionPanel.validationMessage.should('contain', 'minimum 2')

// Select nodes
wizardPage.canvas.nodeIsSelectable('adapter-1').click()
wizardPage.canvas.nodeIsSelectable('adapter-2').click()

// Check constraints
wizardPage.selectionPanel.selectedCount.should('contain', '2 selected')
wizardPage.selectionPanel.nextButton.should('not.be.disabled')
```

**Test Path:**

1. ✅ Selection panel visible
2. ✅ Shows "0 selected, minimum 2 required"
3. ✅ Allowed nodes are selectable
4. ✅ Disallowed nodes are disabled/hidden
5. ✅ Clicking node adds to selection
6. ✅ Selected count updates
7. ✅ Next enabled when >= 2 selected
8. ✅ Can deselect nodes

---

### Step 1: Configure Combining Logic

**Purpose:** Configure how data sources are combined

**DOM State:**

- Progress bar: "Step 2 of 2"
- Progress: 100%
- Configuration form: Combiner settings
- Complete button visible

**Page Object:**

```typescript
wizardPage.progressBar.container.should('contain', 'Step 2')
wizardPage.combinerConfig.configForm.should('be.visible')
// Configure combining logic...
wizardPage.progressBar.completeButton.click()
```

---

## Critical Test Paths

### Path 1: Happy Path - Complete Wizard

```typescript
it('should complete adapter wizard successfully', () => {
  // 1. Start wizard
  wizardPage.createEntityButton.click()
  wizardPage.wizardMenu.selectOption('ADAPTER')

  // 2. Step 0: Ghost Preview
  wizardPage.progressBar.container.should('contain', 'Step 1 of 3')
  wizardPage.canvas.ghostNode.should('be.visible')
  wizardPage.progressBar.nextButton.click()

  // 3. Step 1: Protocol Selection
  wizardPage.progressBar.container.should('contain', 'Step 2 of 3')
  cy.wait('@getProtocols')
  wizardPage.adapterConfig.selectProtocol('HTTP')
  wizardPage.progressBar.nextButton.click()

  // 4. Step 2: Configuration
  wizardPage.progressBar.container.should('contain', 'Step 3 of 3')
  wizardPage.adapterConfig.setAdapterName('Test Adapter')
  wizardPage.progressBar.completeButton.click()

  // 5. Success
  cy.wait('@createAdapter')
  wizardPage.completion.successMessage.should('be.visible')
})
```

### Path 2: Cancel Wizard

```typescript
it('should cancel wizard and clean up', () => {
  wizardPage.startAdapterWizard()
  wizardPage.canvas.ghostNode.should('be.visible')

  wizardPage.progressBar.cancelButton.click()

  wizardPage.progressBar.container.should('not.exist')
  wizardPage.canvas.ghostNode.should('not.exist')
  wizardPage.createEntityButton.should('not.be.disabled')
})
```

### Path 3: Selection Constraints (Combiner)

```typescript
it('should enforce selection constraints', () => {
  wizardPage.startCombinerWizard()

  // Initially disabled
  wizardPage.selectionPanel.nextButton.should('be.disabled')
  wizardPage.selectionPanel.validationMessage.should('contain', 'minimum 2')

  // Select 1 - still disabled
  wizardPage.canvas.selectNode('adapter-1')
  wizardPage.selectionPanel.selectedCount.should('contain', '1')
  wizardPage.selectionPanel.nextButton.should('be.disabled')

  // Select 2 - now enabled
  wizardPage.canvas.selectNode('adapter-2')
  wizardPage.selectionPanel.selectedCount.should('contain', '2')
  wizardPage.selectionPanel.nextButton.should('not.be.disabled')
})
```

### Path 4: Back Navigation

```typescript
it('should navigate back through steps', () => {
  wizardPage.startAdapterWizard()
  wizardPage.progressBar.nextButton.click()

  wizardPage.progressBar.container.should('contain', 'Step 2')
  wizardPage.progressBar.backButton.should('be.visible')

  wizardPage.progressBar.backButton.click()

  wizardPage.progressBar.container.should('contain', 'Step 1')
  wizardPage.progressBar.backButton.should('not.exist')
})
```

### Path 5: Accessibility at Each Step

```typescript
it('should be accessible at each step', () => {
  // Step 0
  wizardPage.startAdapterWizard()
  cy.injectAxe()
  wizardPage.progressBar.container.then(($progress) => {
    cy.checkAccessibility($progress[0])
  })

  // Step 1
  wizardPage.progressBar.nextButton.click()
  wizardPage.adapterConfig.protocolSelector.then(($selector) => {
    cy.checkAccessibility($selector[0])
  })

  // Step 2
  wizardPage.adapterConfig.selectProtocol('HTTP')
  wizardPage.progressBar.nextButton.click()
  wizardPage.adapterConfig.configForm.then(($form) => {
    cy.checkAccessibility($form[0])
  })
})
```

---

## Page Object Design Guide

### Structure

```typescript
class WizardPage {
  // Trigger
  get createEntityButton() { ... }
  wizardMenu = { selectOption(type) { ... } }

  // Progress Bar
  progressBar = {
    get container() { ... }
    get nextButton() { ... }
    get backButton() { ... }
    get completeButton() { ... }
    get cancelButton() { ... }
  }

  // Canvas (Ghost Nodes)
  canvas = {
    get ghostNode() { ... }
    get ghostEdges() { ... }
    node(nodeId) { ... }
    selectNode(nodeId) { ... }
    nodeIsSelectable(nodeId) { ... }
    nodeIsRestricted(nodeId) { ... }
  }

  // Selection Panel
  selectionPanel = {
    get panel() { ... }
    get selectedCount() { ... }
    get selectedNodesList() { ... }
    get validationMessage() { ... }
    get nextButton() { ... }
  }

  // Configuration (Adapter)
  adapterConfig = {
    get protocolSelector() { ... }
    selectProtocol(name) { ... }
    get adapterNameInput() { ... }
    setAdapterName(name) { ... }
    get configForm() { ... }


  // Utility Methods
  startAdapterWizard() { ... }
  startCombinerWizard() { ... }
}
```

### Selector Priority

1. **First:** `[data-testid="exact-name"]`
2. **Second:** `[aria-label="..."]`
3. **Third:** `[role="..."]`
4. **Last:** CSS selectors (as fallback)

Example:

```typescript
get nextButton() {
  return cy.get([
    '[data-testid="wizard-next-button"]',
    '[aria-label*="Next"]',
    'button'
  ].join(', ')).first()
}
```

---

## Common Gotchas & Solutions

### 1. Progress Bar Uses Panel Position

**Issue:** Progress bar is a React Flow Panel, not a regular div  
**Solution:** Use `[data-testid="wizard-progress-bar"]` or look for Panel at bottom-center

### 2. Ghost Nodes Use data-ghost Attribute

**Issue:** Can't use standard node selectors  
**Solution:** Use `[data-ghost="true"]` selector

### 3. Button Changes (Next → Complete)

**Issue:** Button test-id changes on last step  
**Solution:** Check for both `wizard-next-button` and `wizard-complete-button`

### 4. Selection Panel vs Progress Bar

**Issue:** Both have "Next" buttons  
**Solution:** Progress bar is in Panel, selection panel is in main/aside

### 5. Menu in Portal

**Issue:** Menu renders in Chakra Portal, not in button hierarchy  
**Solution:** Select menu items directly, not within button

---

## API Interactions

### Adapter Wizard

| Step | API Call                                        | Method | Response          |
| ---- | ----------------------------------------------- | ------ | ----------------- |
| 1    | `/api/v1/management/protocol-adapters/types`    | GET    | List of protocols |
| 2    | `/api/v1/management/protocol-adapters/adapters` | GET    | Existing adapters |
| 3    | `/api/v1/management/protocol-adapters/adapters` | POST   | Created adapter   |

### Combiner Wizard

| Step | API Call                                     | Method | Response             |
| ---- | -------------------------------------------- | ------ | -------------------- |
| 0    | `/api/v1/management/protocol-adapters/types` | GET    | For capability check |
| 1    | `/api/v1/management/combiners`               | POST   | Created combiner     |

---

## Summary: Quick Reference

| Component           | Key Selectors                               |
| ------------------- | ------------------------------------------- |
| **Trigger**         | `[data-testid="create-entity-button"]`      |
| **Menu Items**      | `[data-testid="wizard-option-{TYPE}"]`      |
| **Progress Bar**    | `[data-testid="wizard-progress-bar"]`       |
| **Next Button**     | `[data-testid="wizard-next-button"]`        |
| **Complete Button** | `[data-testid="wizard-complete-button"]`    |
| **Cancel Button**   | `[data-testid="wizard-cancel-button"]`      |
| **Ghost Nodes**     | `[data-ghost="true"]`                       |
| **Ghost Edges**     | `[data-ghost-edge="true"]`                  |
| **Selection Panel** | `[data-testid="wizard-selection-panel"]`    |
| **Adapter Form**    | `[data-testid="adapter-config-form"], form` |

---

**This document should be updated when:**

- New wizard types are added
- Step configurations change
