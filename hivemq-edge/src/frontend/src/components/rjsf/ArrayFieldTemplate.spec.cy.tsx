import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import type { ArrayFieldTemplateProps } from '@rjsf/utils'
import { Heading, Text } from '@chakra-ui/react'

const mockTemplateProps: ArrayFieldTemplateProps = {
  canAdd: true,
  // @ts-ignore
  idSchema: {
    $id: 'root_items_0_userProperties',
  },
  // @ts-ignore
  items: [{ children: <Text>The first item</Text>, key: '1', index: 0 }],
  schema: {
    title: 'MQTT User Properties',
  },
  registry: {
    translateString: (string) => string,
    // @ts-ignore
    templates: {
      ArrayFieldDescriptionTemplate: () => <Text data-testid="template-description">Description</Text>,
      ArrayFieldTitleTemplate: () => <Heading data-testid="template-title">Title</Heading>,
      ArrayFieldItemTemplate: (props) => (
        <Text role="listitem" data-testid="template-items">
          The property field item {props.index} goes there
        </Text>
      ),
    },
  },
}

describe('ArrayFieldTemplate', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should render properly and be accessible', () => {
    const onAddClick = cy.stub().as('onAddClick')
    cy.injectAxe()

    cy.mountWithProviders(<ArrayFieldTemplate {...mockTemplateProps} onAddClick={onAddClick} />)
    cy.getByTestId('template-title').should('contain.text', 'Title')
    cy.getByTestId('template-description').should('contain.text', 'Description')
    cy.get('[role="list"] [role="listitem"]').should('have.length', 1)
    cy.getByTestId('template-items').should('contain.text', 'The property field item 0 goes there')

    cy.get('@onAddClick').should('not.have.been.called')
    cy.getByTestId('array-item-add').should('contain.text', 'Add Item').click()
    cy.get('@onAddClick').should('have.been.called')

    cy.checkAccessibility()
  })

  describe('Add Item Button', () => {
    it('should not render the button', () => {
      cy.mountWithProviders(<ArrayFieldTemplate {...mockTemplateProps} canAdd={false} />)
      cy.getByTestId('array-item-add').should('not.exist')
    })

    it('should render a custom title', () => {
      const onAddClick = cy.stub().as('onAddClick')
      const uiSchema = {
        items: {
          'ui:addButton': 'Add a user property',
        },
      }
      cy.mountWithProviders(<ArrayFieldTemplate {...mockTemplateProps} uiSchema={uiSchema} onAddClick={onAddClick} />)
      cy.get('@onAddClick').should('not.have.been.called')
      cy.getByTestId('array-item-add').should('contain.text', 'Add a user property').click()
      cy.get('@onAddClick').should('have.been.called')
    })
  })
})
