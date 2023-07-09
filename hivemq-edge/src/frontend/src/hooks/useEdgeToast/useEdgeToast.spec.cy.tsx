/// <reference types="cypress" />

import { useEdgeToast } from './useEdgeToast.tsx'
import { Button } from '@chakra-ui/react'
import { ApiError } from '@/api/__generated__'
import { ApiResult } from '@/api/__generated__/core/ApiResult.ts'

const result: ApiResult = {
  url: 'http://fake.url.com',
  ok: false,
  status: 400,
  statusText: 'Bad Request',
  body: {
    errors: [
      {
        fieldName: 'id',
        title: 'Invalid user supplied data',
        detail: 'Unable to change the id of a bridge, this field is immutable',
      },
      {
        fieldName: 'bridge',
        title: 'Invalid user supplied data',
        detail: 'Bridge did not exist to update',
      },
    ],
  },
}

// @ts-ignore
const MOCK_API_ERROR = new ApiError(undefined, result, 'this is a mistake')

const TestingComponent = () => {
  const { successToast, errorToast } = useEdgeToast()
  return (
    <div>
      <Button data-testid={'trigger-success'} onClick={() => successToast({ title: 'This is a success' })}>
        successToast
      </Button>
      <Button
        data-testid={'trigger-error'}
        onClick={() =>
          errorToast({ title: 'This is an error', description: 'And the error is this one' }, MOCK_API_ERROR)
        }
      >
        errorToast
      </Button>
    </div>
  )
}

describe('NamespaceForm', () => {
  beforeEach(() => {
    cy.viewport(800, 400)
  })

  it('should render success toast properly', () => {
    cy.mountWithProviders(<TestingComponent />)

    cy.getByTestId('trigger-success').click()
    cy.get('[role="status"]').should('have.length', 1)
    cy.get('[role="status"]')
      .eq(0)
      .should('be.visible')
      .find("div[data-status='success']")
      .should('contain.text', 'This is a success')
  })

  it('should render error toast properly', () => {
    cy.mountWithProviders(<TestingComponent />)

    cy.getByTestId('trigger-error').click()
    cy.get('[role="status"]').should('have.length', 1)
    cy.get('[role="status"]')
      .eq(0)
      .should('be.visible')
      .find("div[data-status='error']")
      .should('contain.text', 'This is an error')

    // cy.getByTestId('trigger-error').click()
    // cy.get('[role="status"]').should('have.length', 2)
  })
})
