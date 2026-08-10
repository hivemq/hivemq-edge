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

  it('should upload a single JSON-Schema file', () => {
    const onUpload = cy.stub().as('onUpload')
    cy.mountWithProviders(<SchemaUploader onUpload={onUpload} />)

    cy.get('#dropzone').selectFile('cypress/fixtures/example.json', { action: 'drag-drop' })
    cy.get('@onUpload').should('have.been.calledOnce')
  })

  // react-dropzone 19 accepts the files that fit under `maxFiles` and rejects only the excess,
  // where 14 rejected the whole batch. Without the single-onDrop guard this drop would upload the
  // first file while also toasting an error for the second.
  it('should reject the whole batch when more than one file is dropped', () => {
    const onUpload = cy.stub().as('onUpload')
    cy.mountWithProviders(<SchemaUploader onUpload={onUpload} />)

    cy.get('#dropzone').selectFile(['cypress/fixtures/example.json', 'cypress/fixtures/example.json'], {
      action: 'drag-drop',
    })

    cy.get('[role="status"] > div').should('have.attr', 'data-status', 'error')
    cy.get('@onUpload').should('not.have.been.called')
  })
})
