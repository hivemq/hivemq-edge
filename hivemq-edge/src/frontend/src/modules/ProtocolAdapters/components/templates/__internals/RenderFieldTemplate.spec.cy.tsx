/// <reference types="cypress" />

import { FieldTemplateProps } from '@rjsf/utils'
import { RenderFieldTemplate } from './RenderFieldTemplate.tsx'
import { FormErrorMessage, FormLabel, Input } from '@chakra-ui/react'

const MOCK_TEXT = 'You will have to type something'
const MOCK_ERROR = 'This is an error message'
const makeMockProps = (props: Partial<FieldTemplateProps>): Partial<FieldTemplateProps> => {
  return {
    ...props,
    children: (
      <>
        <FormLabel>{props.label}</FormLabel>
        <Input value={'a dumb value'} />
      </>
    ),
    errors: <FormErrorMessage>{props.rawErrors?.join(', ')}</FormErrorMessage>,
    description: <div>{props.rawDescription}</div>,
  }
}

const ff: Partial<FieldTemplateProps> = {
  id: 'my-id',
  label: 'A dummy form control',
  readonly: false,
  rawErrors: [MOCK_ERROR],
  rawDescription: MOCK_TEXT,
  displayLabel: true,
}

describe('CustomFieldTemplate', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render the error message ', () => {
    const { children, ...rest } = makeMockProps(ff)
    cy.injectAxe()

    // @ts-ignore
    cy.mountWithProviders(<RenderFieldTemplate {...rest} children={children} />)
    cy.get('[role="group"]').should('not.contain.text', MOCK_TEXT)
    cy.get('[role="group"]').should('contain.text', MOCK_ERROR)
    cy.checkAccessibility()
  })

  it('should render the helper text', () => {
    const { children, ...rest } = makeMockProps({ ...ff, rawErrors: undefined })
    cy.injectAxe()

    // @ts-ignore
    cy.mountWithProviders(<RenderFieldTemplate {...rest} children={children} />)
    cy.get('[role="group"]').should('contain.text', MOCK_TEXT)
    cy.get('[role="group"]').should('not.contain.text', MOCK_ERROR)
    cy.checkAccessibility()
  })
})
