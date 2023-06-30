/// <reference types="cypress" />
import ErrorMessage from './ErrorMessage.tsx'
import { ProblemDetails } from '@/api/types/http-problem-details'

const MOCK_STATUS_TEXT = 'This is a test'
const MOCK_ERROR: ProblemDetails = { title: 'This is a title', status: 401 }

describe('ErrorMessage', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })
  it('should renders', () => {
    cy.mountWithProviders(<ErrorMessage type={MOCK_STATUS_TEXT} message={MOCK_ERROR.title} />)
    cy.get('.chakra-alert__title').should('contain.text', 'This is a test')
    cy.get('.chakra-alert__desc').should('contain.text', 'This is a title')
    cy.get("[role='alert']").should('have.attr', 'data-status', 'error')
  })
})
