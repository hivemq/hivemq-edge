/// <reference types="cypress" />
import { Button } from '@chakra-ui/react'
import { MOCK_BREADCRUMB, MOCK_NAMESPACE } from '@/__test-utils__/mocks.ts'

import NamespaceForm from './NamespaceForm.tsx'

describe('NamespaceForm', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<NamespaceForm onSubmit={cy.stub} defaultValues={MOCK_NAMESPACE} />)
    cy.get('#unifiedNamespace-enterprise').should('have.value', MOCK_BREADCRUMB[0])
    cy.get('#unifiedNamespace-site').should('have.value', MOCK_BREADCRUMB[1])
    cy.get('#unifiedNamespace-area').should('have.value', MOCK_BREADCRUMB[2])
    cy.get('#unifiedNamespace-productionLine').should('have.value', MOCK_BREADCRUMB[3])
    cy.get('#unifiedNamespace-workCell').should('have.value', MOCK_BREADCRUMB[4])
  })

  it('should submit', () => {
    const mockOnSubmit = cy.stub().as('onSubmit')

    cy.mountWithProviders(
      <div>
        <NamespaceForm onSubmit={mockOnSubmit} defaultValues={MOCK_NAMESPACE} />
        <Button variant="primary" type={'submit'} form="namespace-form" data-testid={'form-submit'}>
          Submit
        </Button>
      </div>
    )
    cy.getByTestId('form-submit').click()

    cy.get('@onSubmit').should(
      'be.calledWith',
      { ...MOCK_NAMESPACE, prefixAllTopics: false, enabled: true },
      Cypress.sinon.match.object
    )
  })

  it('should change options', () => {
    const mockOnSubmit = cy.stub().as('onSubmit')

    cy.mountWithProviders(
      <div>
        <NamespaceForm onSubmit={mockOnSubmit} defaultValues={MOCK_NAMESPACE} />
        <Button variant="primary" type={'submit'} form="namespace-form" data-testid={'form-submit'}>
          Submit
        </Button>
      </div>
    )
    cy.getByTestId('unifiedNamespace-prefixAllTopics').click()
    cy.getByTestId('form-submit').click()

    cy.get('@onSubmit').should(
      'be.calledWith',
      { ...MOCK_NAMESPACE, prefixAllTopics: true, enabled: true },
      Cypress.sinon.match.object
    )
  })

  it('should edit', () => {
    const mockOnSubmit = cy.stub().as('onSubmit')

    cy.mountWithProviders(
      <div>
        <NamespaceForm onSubmit={mockOnSubmit} defaultValues={MOCK_NAMESPACE} />
        <Button variant="primary" type={'submit'} form="namespace-form" data-testid={'form-submit'}>
          Submit
        </Button>
      </div>
    )
    cy.get('#unifiedNamespace-enterprise').clear()
    cy.get('#unifiedNamespace-enterprise').type('one')
    cy.get('#unifiedNamespace-site').clear()
    cy.get('#unifiedNamespace-site').type('two')
    cy.get('#unifiedNamespace-area').clear()
    cy.get('#unifiedNamespace-area').type('three')
    cy.get('#unifiedNamespace-productionLine').clear()
    cy.get('#unifiedNamespace-productionLine').type('four')
    cy.get('#unifiedNamespace-workCell').clear()
    cy.get('#unifiedNamespace-workCell').type('five')
    cy.getByTestId('form-submit').click()

    cy.get('@onSubmit').should(
      'be.calledWith',
      {
        enterprise: 'one',
        site: 'two',
        area: 'three',
        productionLine: 'four',
        workCell: 'five',
        prefixAllTopics: false,
        enabled: true,
      },
      Cypress.sinon.match.object
    )
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<NamespaceForm onSubmit={cy.stub} defaultValues={MOCK_NAMESPACE} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: NamespaceForm')
  })
})
