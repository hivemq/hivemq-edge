import { OutwardSubscription } from '@/modules/Subscriptions/types.ts'
import MappingEditor from './MappingEditor.tsx'

const MOCK_SUBS: OutwardSubscription = {
  node: 'my-node',
  'mqtt-topic': ['my-topic'],
  mapping: [{ source: ['dropped-property'], destination: 'Second String' }],
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return <h2>{children}</h2>
}

describe('MappingEditor', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <MappingEditor topic="sssss" showTransformation={false} onChange={cy.stub()} mapping={MOCK_SUBS.mapping} />
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

    cy.mountWithProviders(
      <MappingEditor topic="sssss" showTransformation={false} onChange={cy.stub()} mapping={MOCK_SUBS.mapping} />,
      { wrapper }
    )

    cy.checkAccessibility()
  })
})
