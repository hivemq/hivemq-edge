import type { DataCombining } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'
import { DestinationSchemaLoader } from './DestinationSchemaLoader'

const mockDataCombining: DataCombining = {
  id: '1f64f351-dcef-4ca1-ad09-26cd07f45be4',
  sources: {
    primary: { id: '', type: DataIdentifierReference.type.TAG },
    tags: [],
    topicFilters: [],
  },
  destination: { topic: 'my/topic', schema: undefined },
  instructions: [],
}

describe('DestinationSchemaLoader', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    const onChangeInstructions = cy.stub().as('onChangeInstructions')
    cy.mountWithProviders(
      <DestinationSchemaLoader
        formData={mockDataCombining}
        onChange={onChange}
        onChangeInstructions={onChangeInstructions}
      />
    )

    cy.get('@onChange').should('have.not.been.called')

    cy.getByTestId('combiner-destination-infer').should('have.text', 'Infer a schema')
    cy.getByTestId('combiner-destination-upload').should('have.text', 'Upload a new schema')
    cy.getByTestId('combiner-destination-download').should('have.text', 'Download the schema')

    cy.get('[role="dialog"]#chakra-modal-destination-schema').should('not.exist')
    cy.getByTestId('combiner-destination-upload').click()
    cy.get('[role="dialog"]#chakra-modal-destination-schema').should('be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <DestinationSchemaLoader formData={mockDataCombining} onChange={cy.stub} onChangeInstructions={cy.stub} />
    )
    cy.checkAccessibility()
  })
})
