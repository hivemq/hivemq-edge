/// <reference types="cypress" />

import { Node } from 'reactflow'
import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { SUGGESTION_TRIGGER_CHAR } from '@datahub/components/interpolation/Suggestion.ts'
import { DataHubNodeType, OperationData } from '@datahub/types.ts'
import { OperationPanel } from './OperationPanel.tsx'

const getWrapperWith = (initNodes: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <MockStoreWrapper
        config={{
          initialState: {
            nodes: initNodes,
          },
        }}
      >
        {children}
        <Button variant="primary" type="submit" form="datahub-node-form">
          SUBMIT
        </Button>
      </MockStoreWrapper>
    )
  }

  return Wrapper
}

describe('OperationPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render a validating form', () => {
    cy.mountWithProviders(<OperationPanel selectedNode="0" onFormSubmit={cy.stub().as('submit')} />, {
      wrapper: getWrapperWith([]),
    })

    cy.get('label#root_id-label').should('contain.text', 'id')
    cy.get('#root_id').type('a123')

    // first select
    cy.get('label#root_functionId-label').should('contain.text', 'Function')
    cy.get('label#root_functionId-label + div').should('contain.text', '')
    cy.get('label#root_functionId-label + div').click()
    cy.get('label#root_functionId-label + div').find("[role='listbox']").find("[role='option']").as('functionSelect')

    cy.get('@functionSelect').eq(0).should('contain.text', 'System.log')
    cy.get('@functionSelect').eq(7).should('contain.text', 'Mqtt.drop')
    cy.get('@functionSelect').eq(7).click()

    cy.get('@submit').should('not.have.been.calledWith')
    cy.get("button[type='submit']").click()
    cy.get('@submit')
      .should('have.been.calledWith', Cypress.sinon.match.object)
      .its('firstCall.args.0')
      .should('deep.include', {
        edit: true,
        status: 'submitted',
      })
      .its('formData')
      .should('deep.include', { functionId: 'Mqtt.drop', id: 'a123' })
  })

  it('should render validation errors', () => {
    cy.mountWithProviders(<OperationPanel selectedNode="0" onFormSubmit={cy.stub().as('submit')} />, {
      wrapper: getWrapperWith([]),
    })

    cy.get('label#root_functionId-label + div').click()
    cy.get('label#root_functionId-label + div').find("[role='listbox']").find("[role='option']").as('functionSelect')

    cy.get('@functionSelect').eq(1).click()

    cy.get('p#root_formData_applyPolicies__description + label').should('have.text', 'Apply Policies')
    cy.get('p#root_formData_applyPolicies__description + label').click()

    cy.get('@submit').should('not.have.been.calledWith')
    cy.get("button[type='submit']").click()
    cy.get('@submit').should('not.have.been.calledWith')

    cy.get('[role="group"]:has(> label#root_id-label) + [role="list"]').as('id_Errors')
    cy.get('@id_Errors').should('contain.text', "must have required property 'id'")

    cy.get('[role="group"]:has(> label#root_formData_topic-label) + [role="list"]').as('topic_Errors')
    cy.get('@topic_Errors').should('contain.text', "must have required property 'Topic'")

    cy.get('[role="alert"][data-status="error"]').should('be.visible')
    cy.get('[role="alert"][data-status="error"] ul li').as('listErrors')
    cy.get('@listErrors').eq(0).should('contain.text', "must have required property 'id'")
    cy.get('@listErrors').eq(1).should('contain.text', "must have required property 'Topic'")
  })

  describe(OperationData.Function.SYSTEM_LOG, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.TOPIC_FILTER,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.SYSTEM_LOG,
        formData: { level: 'DEBUG', message: 'a message' },
      },
    }

    it('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })

      cy.get('h2').should('contain.text', 'System.log')
      cy.get('label#root_formData_level-label').should('contain.text', 'Log Level')
      cy.get('label#root_formData_level-label + div').should('contain.text', 'DEBUG')
      cy.get('label#root_formData_level-label + div').click()
      cy.get('label#root_formData_level-label + div')
        .find("[role='listbox']")
        .find("[role='option']")
        .as('logLevelSelect')
      cy.get('@logLevelSelect').should('have.length', 5)
      cy.get('@logLevelSelect').eq(4).should('contain.text', 'TRACE')
      cy.get('@logLevelSelect').eq(4).click()
      cy.get('label#root_formData_level-label + div').should('contain.text', 'TRACE')

      // Need a better (and shorter) way of testing it display the right widget
      cy.get('#root_formData_message').type(`A new topic ${SUGGESTION_TRIGGER_CHAR}`)
      cy.getByTestId('interpolation-container').should('be.visible')
      cy.get('#root_formData_message').type('{esc}')
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.checkAccessibility()
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.SYSTEM_LOG}`)
    })
  })

  describe(OperationData.Function.DELIVERY_REDIRECT, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.TOPIC_FILTER,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.DELIVERY_REDIRECT,
        formData: { topic: 'a/simple/topic', applyPolicies: true },
      },
    }

    it('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.get('h2').should('contain.text', 'Delivery.redirectTo')
      cy.get('label#root_formData_topic-label').should('contain.text', 'Topic')
      cy.get('label#root_formData_topic-label + input').should('contain.value', 'a/simple/topic')
      cy.get('label:has(> input#root_formData_applyPolicies) ')
        .as('topic_Errors')
        .should('contain.text', 'Apply Policies')
        .should('have.attr', 'data-checked')
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.checkAccessibility()
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.DELIVERY_REDIRECT}`)
    })
  })

  describe(OperationData.Function.MQTT_USER_PROPERTY, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.OPERATION,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.MQTT_USER_PROPERTY,
        formData: {
          name: 'key',
          value: 'value',
        },
      },
    }

    it('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.get('h2').should('contain.text', 'Mqtt.UserProperties.add')
      cy.get('label#root_formData_name-label').should('contain.text', 'Property Name')
      cy.get('label#root_formData_name-label + input').should('contain.value', 'key')
      cy.get('label#root_formData_value-label').should('contain.text', 'Property Value')
      cy.get('label#root_formData_value-label + input').should('contain.value', 'value')
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.checkAccessibility()
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.MQTT_USER_PROPERTY}`)
    })
  })

  describe(OperationData.Function.SERDES_DESERIALIZE, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.OPERATION,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.SERDES_DESERIALIZE,
        formData: {
          schemaId: 'schema-id',
          schemaVersion: 'version1',
        },
      },
    }

    it('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.get('h2').should('contain.text', 'Serdes.deserialize')
      cy.get('label#root_formData_schemaId-label').should('contain.text', 'Schema ID')
      cy.get('label#root_formData_schemaId-label + input').should('contain.value', 'schema-id')
      cy.get('label#root_formData_schemaVersion-label').should('contain.text', 'Schema Version')
      cy.get('label#root_formData_schemaVersion-label + input').should('contain.value', 'version1')
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.checkAccessibility()
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.SERDES_DESERIALIZE}`)
    })
  })
})
