/// <reference types="cypress" />

import { type UseDisclosureProps, Button } from '@chakra-ui/react'
import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import TagSchemaDrawer from './TagSchemaDrawer'

const mocTag = MOCK_DEVICE_TAGS('opcua-1', MockAdapterType.OPC_UA)[0]

const trigger: (disclosureProps: UseDisclosureProps) => JSX.Element = ({ onOpen: onOpenArrayDrawer }) => (
  <Button data-testid="dev-trigger" onClick={onOpenArrayDrawer}>
    The trigger
  </Button>
)

describe('TagSchemaDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/management/protocol-adapters/writing-schema/**', GENERATE_DATA_MODELS(true, 'test'))
  })

  it('should render properly', () => {
    cy.mountWithProviders(<TagSchemaDrawer adapterId="test" tag={mocTag} trigger={trigger} />)

    cy.get('[role="dialog"]#chakra-modal-tag-schema').should('not.exist')

    cy.getByTestId('dev-trigger').click()
    cy.get('[role="dialog"]#chakra-modal-tag-schema').within(() => {
      cy.get('header').should('have.text', 'Manage the schema for the tag')
      cy.get('footer').within(() => {
        cy.get('button').as('close').should('have.text', 'Close')
      })
    })
    cy.get('@close').click()
    cy.get('[role="dialog"]#chakra-modal-tag-schema').should('not.exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TagSchemaDrawer adapterId="test" tag={mocTag} trigger={trigger} />)
    cy.getByTestId('dev-trigger').click()

    cy.checkAccessibility()
  })
})
