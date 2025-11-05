/// <reference types="cypress" />

import type { ProblemDetails } from '@/api/__generated__'
import ApiErrorToastDevMode from '@datahub/components/helpers/ApiErrorToastDevMode.tsx'

describe('ApiErrorToastDevMode', () => {
  const mockError: ProblemDetails = {
    type: 'DataPolicyInvalid',
    title: 'DataPolicy is invalid',
    detail: 'DataPolicy failed validation',
    status: 400,
    errors: [
      {
        detail: 'Unsupported value',
        parameter:
          "produced by policy validation: Field 'onSuccess.pipeline[0].arguments.level' has unsupported value 'dddd'. Supported values are [ERROR, WARN, INFO, DEBUG, TRACE].",
      },
    ],
  }

  it('should render properly', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ApiErrorToastDevMode message="This is wrong" body={mockError} />)

    cy.getByTestId('content-toggle').click()
    cy.get('[role="list"]').should('be.visible')
    cy.get('[role="list"]').should('contain.text', 'DataPolicy failed validation')
    cy.get('[role="list"]').should('contain.text', 'Unsupported value')
    cy.get('[role="list"]').should('contain.text', 'Supported values are [ERROR, WARN, INFO, DEBUG, TRACE].')

    cy.checkAccessibility()
  })
})
