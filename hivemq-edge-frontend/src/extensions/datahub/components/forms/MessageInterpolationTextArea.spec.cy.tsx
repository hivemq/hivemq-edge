import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { MOCK_INTERPOLATION_VARIABLES } from '@datahub/api/hooks/DataHubInterpolationService/__handlers__'
import { DataHubNodeType, DesignerStatus } from '@datahub/types.ts'
import type { FC } from 'react'
import type { JSONSchema7 } from 'json-schema'
import type { FormProps } from '@rjsf/core'
import type { UiSchema } from '@rjsf/utils'
import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'

import { MessageInterpolationTextArea } from '@datahub/components/forms/MessageInterpolationTextArea.tsx'

const mockUISchema: UiSchema = {
  message: {
    'ui:widget': 'datahub:message-interpolation',
  },
}

const mockSchema: JSONSchema7 = {
  properties: {
    message: {
      type: 'string',
      minLength: 5,
    },
  },
}

type FormPropsStubs = Pick<FormProps<unknown>, 'onChange'>
const MockForm: FC<FormPropsStubs> = (props) => (
  <CustomFormTesting
    schema={mockSchema}
    uiSchema={mockUISchema}
    widgets={{
      'datahub:message-interpolation': MessageInterpolationTextArea,
    }}
    formData={{ message: 'Hello world' }}
    {...props}
  />
)

const getWrapperFor = (type: DataHubNodeType.DATA_POLICY | DataHubNodeType.BEHAVIOR_POLICY) => {
  const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
    <MockStoreWrapper
      config={{
        initialState: {
          status: DesignerStatus.DRAFT,
          type: type || DataHubNodeType.DATA_POLICY,
        },
      }}
    >
      {children}
    </MockStoreWrapper>
  )
  return wrapper
}

describe('MessageInterpolationTextArea', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the textarea and handle input', () => {
    const onChange = cy.stub().as('onChange')
    cy.intercept('/api/v1/data-hub/interpolation-variables', MOCK_INTERPOLATION_VARIABLES).as('getVariables')
    cy.mountWithProviders(<MockForm onChange={onChange} />, {
      wrapper: getWrapperFor(DataHubNodeType.DATA_POLICY),
    })

    cy.get("label[for='root_message']").should('exist')
    cy.getByTestId('root_message').should('not.have.attr', 'data-invalid')
    cy.getByTestId('root_message').within(() => {
      cy.get('[role="textbox"][aria-labelledby]').as('container')
    })

    cy.get('@container').should('have.attr', 'aria-placeholder', 'Write the message')
    cy.get('@container').within(() => {
      cy.get('.tiptap[role="textbox"]').as('editor')
    })

    cy.get('@editor').should('have.attr', 'contenteditable', 'true')
    cy.get('@editor').should('have.text', 'Hello world')

    cy.get('@editor').clear()
    cy.get('@editor').type('123')
    cy.getByTestId('root_message').should('have.attr', 'data-invalid')
    cy.get('#root_message__error').should('contain.text', 'must NOT have fewer than 5 characters')
    cy.get('@onChange').should('have.been.called')

    cy.getByTestId('interpolation-container').should('not.exist')

    cy.get('@editor').clear()
    cy.get('@editor').type('This is a message for $')

    cy.getByTestId('interpolation-container').should('be.visible')
    cy.getByTestId('interpolation-container').within(() => {
      cy.get('button').should('have.length', 5)
    })
    cy.get('body').type('{downArrow}{downArrow}{downArrow}')
    cy.getByTestId('interpolation-container').within(() => {
      cy.get('button').eq(3).should('have.attr', 'aria-selected', 'true')
    })
    cy.get('body').type('{enter}')

    cy.get('@editor').within(() => {
      cy.get("span[data-type='mention']").should('have.length', 1)
      cy.get("span[data-type='mention']").should('contain.text', '$validationResult')
    })

    cy.get('@editor').type(' at timestamp $time')
    cy.getByTestId('interpolation-container').within(() => {
      cy.get('button').should('have.length', 1)
    })
    cy.get('body').type('{enter}')
    cy.get('@editor').within(() => {
      cy.get("span[data-type='mention']").should('have.length', 2)
      cy.get("span[data-type='mention']").eq(0).should('contain.text', '$validationResult')
      cy.get("span[data-type='mention']").eq(1).should('contain.text', '$timestamp')
    })

    cy.get('@onChange').then((stub) => {
      const onChangeStub = stub as unknown as Cypress.Agent<sinon.SinonStub>
      const lastCallArgs = onChangeStub.args.slice(-1)[0]

      expect(lastCallArgs[0]).to.deep.include({
        // There is a weird non-breaking space character in the message after the first interpolation
        formData: { message: 'This is a message for ${validationResult} at timestamp ${timestamp} ' },
      })
      expect(lastCallArgs[1]).to.equal('root_message')
    })
  })

  it.only('should be accessible', () => {
    cy.injectAxe()
    cy.intercept('/api/v1/data-hub/interpolation-variables', MOCK_INTERPOLATION_VARIABLES).as('getVariables')
    cy.mountWithProviders(<MockForm />, {
      wrapper: getWrapperFor(DataHubNodeType.DATA_POLICY),
    })
    cy.getByTestId('root_message').within(() => {
      cy.get('[role="textbox"][aria-labelledby]').as('container')
    })

    cy.get('@container').within(() => {
      cy.get('.tiptap[role="textbox"]').as('editor')
    })
    cy.get('@editor').type('This is a message for $')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
        regionßå: { enabled: false },
      },
    })
  })
})
