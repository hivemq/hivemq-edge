import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { MOCK_INTERPOLATION_VARIABLES } from '@datahub/api/hooks/DataHubInterpolationService/__handlers__'
import { DataHubNodeType, DesignerStatus } from '@datahub/types.ts'
import type { FC, PropsWithChildren } from 'react'
import { FormControl, FormLabel } from '@chakra-ui/react'

import { Editor } from '@datahub/components/interpolation/Editor.tsx'
import { SUGGESTION_TRIGGER_CHAR } from '@datahub/components/interpolation/Suggestion.ts'

const Wrapper: FC<PropsWithChildren> = ({ children }) => {
  return (
    <MockStoreWrapper
      config={{
        initialState: {
          status: DesignerStatus.DRAFT,
          type: DataHubNodeType.DATA_POLICY,
        },
      }}
    >
      <FormControl>
        <FormLabel htmlFor="my-id" id="my-label-id">
          A text editor with interpolation
        </FormLabel>
        {children}
      </FormControl>
    </MockStoreWrapper>
  )
}

describe('Editor', () => {
  beforeEach(() => {
    cy.viewport(400, 600)
  })

  it('should render the editor', () => {
    cy.intercept('/api/v1/data-hub/interpolation-variables', MOCK_INTERPOLATION_VARIABLES).as('getVariables')
    cy.mountWithProviders(<Editor id="my-id" labelId="my-label-id" value="This is a test ${validationResult}" />, {
      wrapper: Wrapper,
    })
    cy.get('#my-id').should('contain.text', `This is a test ${SUGGESTION_TRIGGER_CHAR}validationResult`)
    cy.get('#my-id').within(() => {
      cy.get('[data-type="mention"]').should('have.length', 1)
      cy.get('[data-type="mention"]').should('have.attr', 'data-id', 'validationResult')
    })
    cy.get('#my-id').click()
    cy.get('#my-id').type('{selectall}')
    cy.get('#my-id').type(`A new topic ${SUGGESTION_TRIGGER_CHAR}`)
    cy.getByTestId('interpolation-container').should('be.visible')
    // cy.getByTestId('interpolation-container').type('{downArrow}')
    cy.getByTestId('interpolation-container').find('button').eq(4).click()
    cy.get('#my-id').should('contain.text', `A new topic ${SUGGESTION_TRIGGER_CHAR}timestamp`)

    cy.get('#my-id').within(() => {
      cy.get('[data-type="mention"]').should('have.length', 1)
      cy.get('[data-type="mention"]').should('have.attr', 'data-id', 'timestamp')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Editor id="my-id" labelId="my-label-id" value="This is a test ${validationResult}" />, {
      wrapper: Wrapper,
    })
    cy.get('#my-id').should('contain.text', `This is a test ${SUGGESTION_TRIGGER_CHAR}validationResult`)

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO Tiptap editor is missing a `name` in its props
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})
