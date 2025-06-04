import DataSourceStep from '@/components/rjsf/BatchModeMappings/components/DataSourceStep.tsx'
import type { BatchModeStore } from '@/components/rjsf/BatchModeMappings/types.ts'
import { MOCK_ID_SCHEMA } from '@/components/rjsf/BatchModeMappings/__test-utils__/store.mocks.ts'

const MOCK_STORE: BatchModeStore = {
  idSchema: MOCK_ID_SCHEMA,
  schema: {},
}

describe('DataSourceStep', () => {
  beforeEach(() => {
    cy.viewport(600, 600)
  })

  it('should render the dropzone', () => {
    cy.mountWithProviders(<DataSourceStep onContinue={cy.stub()} store={MOCK_STORE} />)
    cy.get('#dropzone > p').should('contain.text', 'Upload a .xlsx, .xls or .csv file')
    cy.get('#dropzone > button').should('contain.text', 'Select file')

    cy.get('#dropzone').selectFile('cypress/fixtures/test-spreadsheet.xlsx', { action: 'drag-drop' })
    cy.get('#dropzone > p').should('contain.text', 'Drop a file here')

    cy.get('[role="status"]').should('be.visible')
    cy.get('[role="status"] > div').should('have.attr', 'data-status', 'success')
    cy.get('[role="status"] > div').should('contain.text', 'test-spreadsheet.xlsx upload successful')
  })

  it('should deliver the data', () => {
    cy.mountWithProviders(<DataSourceStep onContinue={cy.stub().as('getWorksheet')} store={MOCK_STORE} />)

    cy.get('#dropzone').selectFile('cypress/fixtures/test-spreadsheet.xlsx', { action: 'drag-drop' })
    cy.get('@getWorksheet')
      .should('have.been.calledWith', Cypress.sinon.match.object)
      .its('firstCall.args.0')
      .its('worksheet')
      .should('deep.equal', [
        {
          a: 1,
          b: 2,
          c: 3,
          d: 4,
        },
        {
          a: 2,
          b: 3,
          c: 3,
          d: 5,
        },
      ])

    // .should('deep.include', {
    //   worksheet: Cypress.sinon.match.array,
    // })
  })
})
