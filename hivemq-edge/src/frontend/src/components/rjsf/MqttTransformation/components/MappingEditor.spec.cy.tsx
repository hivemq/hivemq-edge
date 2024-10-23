import { OutwardMapping } from '@/modules/Mappings/types.ts'
import MappingEditor from './MappingEditor.tsx'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'

const MOCK_SUBS: OutwardMapping = {
  tag: 'my-node',
  mqttTopicFilter: 'my-topic',
  fieldMapping: [{ source: { propertyPath: 'dropped-property' }, destination: { propertyPath: 'Second String' } }],
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <h2>{children}</h2>
}

describe('MappingEditor', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/domain/tags/schema?*', GENERATE_DATA_MODELS(true, 'test'))

    cy.mountWithProviders(
      <MappingEditor topic="test" showTransformation={false} onChange={cy.stub()} mapping={MOCK_SUBS.fieldMapping} />
    )

    cy.get('h3').should('have.text', 'Properties to set')
    cy.getByTestId('auto-mapping').should('have.text', 'Auto-mapping')

    // loading
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.getByTestId('loading-spinner').should('not.exist')

    cy.get('[role=list]').find('li').should('have.length', 6)
    cy.getByTestId('mapping-instruction-dropzone').eq(0).should('have.text', 'Drag a source property here')
    cy.getByTestId('mapping-instruction-dropzone').eq(1).should('have.text', 'dropped-property')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.intercept('/api/v1/management/domain/tags/schema?*', GENERATE_DATA_MODELS(true, 'test'))
    cy.mountWithProviders(
      <MappingEditor topic="test" showTransformation={false} onChange={cy.stub()} mapping={MOCK_SUBS.mapping} />,
      { wrapper }
    )

    cy.checkAccessibility()
  })
})
