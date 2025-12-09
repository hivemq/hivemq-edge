# DataHub Resource Edit Flow - E2E Testing Quick Reference

> ⚠️ **TEMPORARY DOCUMENT** - This is a consolidated quick reference created during development. It may be too verbose and needs refinement. Once tests are working, distill key learnings into shorter guidelines.

**Task:** 37937-datahub-resource-edit-flow  
**Created:** December 2, 2025  
**Purpose:** Quick reference for writing and fixing E2E tests for DataHub resource management

---

## 🚀 PRAGMATIC APPROACH: Test One at a Time

**⏱️ Cypress E2E tests are SLOW** - Running all tests takes minutes. Use this incremental approach:

### Step-by-Step Strategy

1. **Use `.only` to run single test**

   ```typescript
   it.only('should create a new JSON schema from Schema table', () => {
     // ... test code
   })
   ```

2. **Run, observe, fix**

   ```bash
   pnpm cypress:run:e2e --spec "cypress/e2e/datahub/resource-edit-flow.spec.cy.ts"
   ```

3. **Apply learnings to all tests** - Don't move to next test until current one passes

4. **Remove `.only` and move to next test** - Repeat process

5. **Final run without `.only`** - Verify all tests pass together

### Why This Approach?

- ✅ Fast feedback loop (1 test ≈ 10-20 seconds)
- ✅ Focused debugging (one problem at a time)
- ✅ Learnings applied incrementally (fix pattern everywhere before moving on)
- ✅ Avoid getting overwhelmed by multiple failures
- ❌ Running all tests takes 2-5 minutes and creates noise

---

## 🎯 Core Principles

### 1. **Use POM Only - Never Direct Selectors**

```typescript
// ❌ WRONG - Direct selector
cy.get('#root_name').type('my-schema')
cy.get('[role="option"]').contains('JSON').click()

// ✅ CORRECT - POM selector
datahubPage.schemaEditor.nameField.type('my-schema')
datahubPage.schemaEditor.typeField.click()
datahubPage.schemaEditor.drawer.within(() => {
  cy.contains('[role="option"]', 'JSON').click()
})
```

**Why:** POM centralizes selectors so test changes are minimal when UI changes.

### 2. **Focus on Test Narrative, Not Implementation**

```typescript
// ❌ WRONG - Implementation-focused
it('should select JSON type from dropdown', () => {
  cy.get('#root_type').click()
  cy.get('[role="option"]').eq(0).click()
})

// ✅ CORRECT - Narrative-focused
it('should create a new JSON schema from Schema table', () => {
  // NARRATIVE: User wants to create a new JSON schema for temperature readings
  // They navigate to schemas, click create, fill in details, and save

  datahubPage.schemasTab.click()
  datahubPage.schemasTable.createButton.click()
  // ... rest of the flow
})
```

**Why:** Tests should describe _what the user does and why_, not _how the UI works_.

### 3. **Monaco Editors Require Special Handling**

```typescript
// ❌ WRONG - Cannot use .type() on Monaco
datahubPage.schemaEditor.definitionEditor.type(jsonSchema)

// ✅ CORRECT - Use Monaco API
monacoEditor.setValue('#root_schemaSource', jsonSchema)
```

**Why:** Monaco uses virtual DOM and web workers - standard Cypress commands don't work.

---

## 📋 Common Patterns

### Creating a New Schema

```typescript
it('should create a new JSON schema from Schema table', () => {
  // NARRATIVE: User creates a schema to validate temperature sensor data

  // Navigate to schemas and open creator
  datahubPage.schemasTab.click()
  datahubPage.schemasTable.createButton.click()

  // Verify drawer opens
  datahubPage.schemaEditor.drawer.should('be.visible')
  datahubPage.schemaEditor.title.should('contain', 'Create New Schema')

  // Fill in name
  datahubPage.schemaEditor.nameField.type('temperature-reading')

  // Select type (React Select - click, then select option)
  datahubPage.schemaEditor.typeField.click()
  datahubPage.schemaEditor.drawer.within(() => {
    cy.contains('[role="option"]', 'JSON').click()
  })

  // Verify version shows DRAFT
  datahubPage.schemaEditor.versionDisplay.should('contain.text', 'DRAFT')

  // Set schema content using Monaco
  const jsonSchema = JSON.stringify(MOCK_SCHEMA_SOURCE, null, 2)
  monacoEditor.setValue('#root_schemaSource', jsonSchema)

  // Save and verify
  cy.intercept('POST', '/api/v1/data-hub/schemas', { ...}).as('createSchema')
  datahubPage.schemaEditor.saveButton.click()
  cy.wait('@createSchema')

  // Verify results
  datahubPage.schemaEditor.drawer.should('not.exist')
  datahubPage.schemasTable.cell(0, 'name').should('contain.text', 'temperature-reading')
})
```

### Editing an Existing Schema

```typescript
it('should edit an existing schema and create new version', () => {
  // NARRATIVE: User needs to update schema to add new field

  // Setup: Create existing schema in mock DB
  const existingSchema = { id: 'my-schema', version: 1, ... }
  mswDB.schema.create({ id: existingSchema.id, json: JSON.stringify(existingSchema) })

  // Open editor from table
  datahubPage.schemasTab.click()
  datahubPage.schemasTable.action(0, 'edit').click()

  // Verify edit mode
  datahubPage.schemaEditor.drawer.should('be.visible')
  datahubPage.schemaEditor.title.should('contain', 'Create New Schema Version')

  // Verify name is readonly
  datahubPage.schemaEditor.nameField.should('have.attr', 'readonly')
  datahubPage.schemaEditor.nameField.should('have.value', 'my-schema')

  // Verify version shows MODIFIED (not DRAFT)
  datahubPage.schemaEditor.versionDisplay.should('contain.text', 'MODIFIED')

  // Modify content
  monacoEditor.setValue('#root_schemaSource', updatedSchema)

  // Save and verify version incremented
  cy.intercept('POST', '/api/v1/data-hub/schemas', { ...version: 2 }).as('updateSchema')
  datahubPage.schemaEditor.saveButton.click()
  cy.wait('@updateSchema')

  datahubPage.toast.shouldContain('version')
})
```

### Creating a Script

```typescript
it('should create a new JavaScript script from Script table', () => {
  // NARRATIVE: User writes transformation function for data processing

  datahubPage.scriptsTab.click()
  datahubPage.scriptsTable.createButton.click()

  datahubPage.scriptEditor.drawer.should('be.visible')
  datahubPage.scriptEditor.nameField.type('temperature-converter')
  datahubPage.scriptEditor.descriptionField.type('Converts Fahrenheit to Celsius')

  // Verify DRAFT version
  datahubPage.scriptEditor.versionDisplay.should('contain.text', 'DRAFT')

  // Write code using Monaco
  const jsCode = `function convert(temp) { return (temp - 32) * 5/9; }`
  monacoEditor.setValue('#root_sourceCode', jsCode)

  // Save and verify
  cy.intercept('POST', '/api/v1/data-hub/scripts', { ...}).as('createScript')
  datahubPage.scriptEditor.saveButton.click()
  cy.wait('@createScript')

  datahubPage.toast.shouldContain('Script')
  datahubPage.scriptsTable.cell(0, 'name').should('contain.text', 'temperature-converter')
})
```

---

## 🔧 POM Reference

### DataHub Page Object

```typescript
// Navigation
datahubPage.schemasTab.click()
datahubPage.scriptsTab.click()
datahubPage.addNewPolicy.click()

// Schema Table
datahubPage.schemasTable.createButton.click()
datahubPage.schemasTable.cell(rowIndex, 'name' | 'type' | 'version')
datahubPage.schemasTable.action(rowIndex, 'edit' | 'download' | 'delete')

// Script Table
datahubPage.scriptsTable.createButton.click()
datahubPage.scriptsTable.cell(rowIndex, 'name' | 'version')
datahubPage.scriptsTable.action(rowIndex, 'edit' | 'download' | 'delete')

// Schema Editor
datahubPage.schemaEditor.drawer
datahubPage.schemaEditor.title
datahubPage.schemaEditor.nameField
datahubPage.schemaEditor.typeField // React Select - click then select option
datahubPage.schemaEditor.versionDisplay // Read-only - check .contain.text()
datahubPage.schemaEditor.definitionEditorContainer // Use monaco.setValue()
datahubPage.schemaEditor.saveButton
datahubPage.schemaEditor.cancelButton

// Script Editor
datahubPage.scriptEditor.drawer
datahubPage.scriptEditor.title
datahubPage.scriptEditor.nameField
datahubPage.scriptEditor.descriptionField
datahubPage.scriptEditor.versionDisplay // Read-only - check .contain.text()
datahubPage.scriptEditor.sourceEditorContainer // Use monaco.setValue()
datahubPage.scriptEditor.saveButton
datahubPage.scriptEditor.cancelButton

// Toast
datahubPage.toast.shouldContain(text)
datahubPage.toast.close()
```

### Monaco Editor POM

```typescript
// Set editor content
monacoEditor.setValue('#root_schemaSource', jsonSchema)
monacoEditor.setValue('#root_sourceCode', jsCode)

// Get editor content
monacoEditor.getValue('#root_schemaSource').should('contain', 'temperature')

// Wait for Monaco to be ready
monacoEditor.waitForEditor('#root_schemaSource')

// Check content
monacoEditor.shouldContain('#root_schemaSource', 'my-text')
```

---

## ⚠️ Common Mistakes & Fixes

### Mistake 1: Using .type() on Monaco

```typescript
// ❌ WRONG
datahubPage.schemaEditor.definitionEditor.type(jsonSchema, { force: true })

// ✅ CORRECT
monacoEditor.setValue('#root_schemaSource', jsonSchema)
```

### Mistake 2: Checking Version Field Value

```typescript
// ❌ WRONG - version field is not an input
datahubPage.schemaEditor.versionField.should('have.value', 'DRAFT')

// ✅ CORRECT - check display text
datahubPage.schemaEditor.versionDisplay.should('contain.text', 'DRAFT')
```

### Mistake 3: React Select with .select()

```typescript
// ❌ WRONG - not a <select> element
datahubPage.schemaEditor.typeField.select('JSON')

// ✅ CORRECT - click to open, then select option
datahubPage.schemaEditor.typeField.click()
datahubPage.schemaEditor.drawer.within(() => {
  cy.contains('[role="option"]', 'JSON').click()
})
```

### Mistake 4: Direct Selectors Instead of POM

```typescript
// ❌ WRONG - bypasses POM
cy.get('[data-testid="schema-create-new-button"]').click()
cy.get('#root_name').type('my-schema')
cy.contains('td', 'my-schema').should('be.visible')

// ✅ CORRECT - use POM
datahubPage.schemasTable.createButton.click()
datahubPage.schemaEditor.nameField.type('my-schema')
datahubPage.schemasTable.cell(0, 'name').should('contain.text', 'my-schema')
```

### Mistake 5: Checking Disabled State

```typescript
// ❌ WRONG - readonly attribute is used, not disabled
datahubPage.schemaEditor.nameField.should('be.disabled')

// ✅ CORRECT - check readonly attribute
datahubPage.schemaEditor.nameField.should('have.attr', 'readonly')
```

---

## 🐛 Error Handling

### Monaco Worker and React Query Errors

```typescript
describe('Schema Resource Management', () => {
  beforeEach(() => {
    // Ignore Monaco worker loading errors and React Query cancelation errors
    Cypress.on('uncaught:exception', (err) => {
      // Return false to prevent test from failing
      return false
    })
  })

  // Tests...
})
```

**Why:** Monaco loads web workers from CDN and React Query may cancel requests - both cause uncaught exceptions in E2E tests.

---

## 📝 Test Narrative Guidelines

### Good Test Narratives

1. **Describe user intent**: "User wants to create a schema for temperature validation"
2. **Explain workflow**: "They navigate to schemas, click create, fill details, save"
3. **Focus on business value**: "This allows them to validate incoming sensor data"

### Bad Test Narratives

1. ❌ "Should click button and type in field"
2. ❌ "Should call API endpoint /api/v1/data-hub/schemas"
3. ❌ "Should update React state when onChange fires"

### Template

```typescript
it('should [user action] [from context]', () => {
  // NARRATIVE: [User wants to / needs to] [achieve goal]
  // [They do this by] [workflow steps]
  // [This enables them to / results in] [outcome]
  // Arrange: Set up test data
  // Act: Perform user actions using POM
  // Assert: Verify outcomes using POM
})
```

---

## 🎓 Key Learnings

1. **POM abstracts implementation** - Tests stay stable when UI changes
2. **Monaco needs special handling** - Use `win.monaco` API in E2E tests
3. **Version display is read-only** - Check text content, not input value
4. **React Select is not <select>** - Click to open, click option to select
5. **Readonly != Disabled** - Check `readonly` attribute for name field in edit mode
6. **Test narrative matters** - Focus on user goals, not technical details
7. **Error handling is essential** - Monaco and React Query cause expected errors in E2E

---

## 📚 Related Documentation

- **Full Testing Plan**: `.tasks/37937-datahub-resource-edit-flow/PHASE_4_E2E_TESTING_PLAN.md`
- **Monaco Guide**: `.tasks/MONACO_TESTING_GUIDE.md`
- **Cypress Guidelines**: `.tasks/CYPRESS_TESTING_GUIDELINES.md`
- **React Select Patterns**: `.tasks/REACT_SELECT_TESTING_PATTERNS.md`
- **DataHub Architecture**: `.tasks/DATAHUB_ARCHITECTURE.md`
