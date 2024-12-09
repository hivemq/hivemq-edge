import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader.tsx'

describe('SchemaUploader', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onUpload = cy.stub().as('onUpload')
    cy.mountWithProviders(<SchemaUploader onUpload={onUpload} />)

    cy.get('#dropzone').as('dropzone')
    cy.get('#dropzone p').should('have.text', 'Upload a JSON-Schema file')
    cy.get('#dropzone button').should('have.text', 'Select file')
  })
})
