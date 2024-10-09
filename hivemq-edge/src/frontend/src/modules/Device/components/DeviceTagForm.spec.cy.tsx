/// <reference types="cypress" />

import DeviceTagForm from '@/modules/Device/components/DeviceTagForm.tsx'
import { ManagerContextType } from '@/modules/Mappings/types.ts'
import { createSchema } from '@/modules/Device/utils/tags.utils.ts'

describe('DeviceTagForm', () => {
  beforeEach(() => {
    cy.viewport(800, 1000)
  })

  it('should render the errors', () => {
    const mockContext: ManagerContextType = { schema: undefined }
    cy.mountWithProviders(<DeviceTagForm context={mockContext} />, {
      routerProps: { initialEntries: [`/node/wrong-adapter`] },
    })

    cy.get('[role="alert"')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'The form cannot be created, due to internal errors')
  })

  it('should render the form', () => {
    const onSubmit = cy.stub().as('onSubmit')

    const mockContext: ManagerContextType = {
      schema: createSchema({ properties: { test: { type: 'string' } } }),
      formData: {
        items: [
          {
            tag: 'opcua-generator/power/off',
            dataPoint: {
              test: 'ns=3;i=1002',
            },
          },
          // {
          //   tag: 'opcua-generator/log/event',
          //   dataPoint: {
          //     test: 'ns=3;i=1008',
          //   },
          // },
        ],
      },
    }

    cy.mountWithProviders(<DeviceTagForm context={mockContext} onSubmit={onSubmit} />)

    cy.get('#root_items__title').should('contain.text', 'List of tags')
    cy.get('#root_items__title + p').should('contain.text', 'The list of all tags defined in the device')
    cy.get('#root_items__title + p + [role="list"] + div button').should('contain.text', 'Add Item')

    cy.get('#root_items__title + p + [role="list"]').as('tagList').should('be.visible')
    cy.get('@tagList').find('[role="listitem"]').should('have.length', 1)

    cy.get('@tagList').eq(0).find('[role="toolbar"] button').eq(0).should('have.attr', 'aria-label', 'Remove')
    cy.get('#root_items_0__title').should('have.text', 'items-0')

    cy.get('@tagList').eq(0).find('[role="group"] > label').eq(0).should('contain.text', 'Tag Name')
    cy.get('@tagList').eq(0).find('[role="group"] > input').eq(0).should('contain.value', `opcua-generator/power/off`)

    cy.get('#root_items__title + p + [role="list"] + div button').click()
    cy.get('@tagList').find('[role="listitem"]').should('have.length', 2)

    cy.get('@tagList').eq(0).find('[role="group"] > label').eq(2).should('contain.text', 'Tag Name')
    cy.get('@tagList').eq(0).find('[role="group"] > input').eq(2).type('1234')
    cy.get('@tagList').eq(0).find('[role="group"] > label').eq(3).should('contain.text', 'test')
    cy.get('@tagList').eq(0).find('[role="group"] > input').eq(3).type('5678')

    cy.get('@onSubmit').should('not.have.been.called')
    cy.get('button[type="submit"]').click()
    cy.get('@onSubmit').should('have.been.calledWith', {
      items: [
        {
          tag: 'opcua-generator/power/off',
          dataPoint: {
            test: 'ns=3;i=1002',
          },
        },
        {
          tag: '1234',
          dataPoint: {
            test: '5678',
          },
        },
      ],
    })
  })
})
