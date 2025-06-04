import { MOCK_NODE_GROUP } from '@/__test-utils__/react-flow/nodes.ts'
import GroupMetadataEditor from '@/modules/Workspace/components/parts/GroupMetadataEditor.tsx'

describe('GroupMetadataEditor', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(
      <GroupMetadataEditor group={{ ...MOCK_NODE_GROUP, position: { x: 50, y: 100 } }} onSubmit={onSubmit} />
    )

    cy.getByTestId('group-metadata-header').should('contain.text', 'Configure the group')
    cy.get('form [role="group"]').as('editor')
    cy.get('@editor').eq(0).find('label').should('contain.text', 'Title')
    cy.get('@editor').eq(0).find('input').should('have.value', 'The group title')

    cy.get('@editor').eq(1).find('legend').should('contain.text', 'Group colour')
    cy.get('@editor').eq(1).find('button').should('have.attr', 'data-color-scheme', 'gray')

    cy.getByTestId('form-submit').should('have.text', 'Save changes').should('be.disabled')
    cy.get('@onSubmit').should('not.have.been.called')

    cy.get('@editor').eq(0).find('input').type(' 123')
    cy.getByTestId('form-submit').should('not.be.disabled')
    cy.getByTestId('form-submit').click()

    cy.get('@onSubmit').should('have.been.calledWithMatch', {
      childrenNodeIds: ['idAdapter', 'idBridge'],
      title: 'The group title 123',
      colorScheme: undefined,
    })
  })

  it('should be accessible', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.injectAxe()
    cy.mountWithProviders(
      <GroupMetadataEditor group={{ ...MOCK_NODE_GROUP, position: { x: 50, y: 100 } }} onSubmit={onSubmit} />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: GroupMetadataEditor')
  })
})
