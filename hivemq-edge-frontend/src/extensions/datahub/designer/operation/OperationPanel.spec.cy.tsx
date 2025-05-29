/// <reference types="cypress" />

import type { Edge, Node } from '@xyflow/react'
import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import { SUGGESTION_TRIGGER_CHAR } from '@datahub/components/interpolation/Suggestion.ts'
import type { FunctionData } from '@datahub/types.ts'
import { DataHubNodeType, OperationData } from '@datahub/types.ts'

import { OperationPanel } from './OperationPanel.tsx'

const getWrapperWith = (initNodes: Node[], initEdges?: Edge[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <MockStoreWrapper
        config={{
          initialState: {
            nodes: initNodes,
            edges: initEdges,
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
    cy.intercept('/api/v1/data-hub/function-specs', {
      items: MOCK_DATAHUB_FUNCTIONS.items.map((specs) => {
        specs.metadata.inLicenseAllowed = true
        return specs
      }),
    }).as('getFunctionSpecs')
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

    cy.get('@functionSelect').eq(0).should('contain.text', 'Mqtt.UserProperties.add')
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

    cy.get('[role="group"]:has(> label[for=root_formData_topic]) + [role="list"]').as('topic_Errors')
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

      // TODO[30464] There is a bug in the selector options that add an extra empty option
      cy.get('@logLevelSelect').should('have.length', 6)
      cy.get('@logLevelSelect').eq(5).should('contain.text', 'TRACE')
      cy.get('@logLevelSelect').eq(5).click()
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
      cy.checkAccessibility(undefined, {
        rules: {
          // TODO Tiptap editor is missing a `name` in its props
          'aria-input-field-name': { enabled: false },
        },
      })
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

    it.only('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.get('h2').should('contain.text', 'Delivery.redirectTo')
      cy.get('label[for=root_formData_topic]').should('contain.text', 'Topic')
      cy.get('label[for=root_formData_topic] + div').should('contain.text', 'a/simple/topic')

      cy.get('label:has(> input#root_formData_applyPolicies) ')
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

  describe(OperationData.Function.SERDES_SERIALIZE, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.OPERATION,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.SERDES_SERIALIZE,
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
      cy.get('h2').should('contain.text', 'Serdes.serialize')
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
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.SERDES_SERIALIZE}`)
    })
  })

  describe(OperationData.Function.METRICS_COUNTER_INC, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.OPERATION,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.METRICS_COUNTER_INC,
        formData: {
          metricName: 'metric-name',
          incrementBy: 12,
        },
      },
    }

    it('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.get('h2').should('contain.text', 'Metrics.Counter.increment')
      cy.get('label#root_formData_metricName-label').should('contain.text', 'Metric Name')
      cy.get('label#root_formData_metricName-label + div > div').should(
        'contain.text',
        'com.hivemq.com.data-hub.custom.counters.'
      )
      cy.get('label#root_formData_metricName-label + div > input').should('contain.value', 'metric-name')
      cy.get('label[for="root_formData_incrementBy"]').should('contain.text', 'Increment By')
      cy.get('label[for="root_formData_incrementBy"] + div >  input').should('contain.value', 12)
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.checkAccessibility()
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.METRICS_COUNTER_INC}`)
    })
  })

  describe(OperationData.Function.MQTT_DISCONNECT, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.OPERATION,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.MQTT_DISCONNECT,
        formData: {},
      },
    }

    it('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.get('h2').should('contain.text', 'Mqtt.disconnect')
      cy.get('[role="group"]:has(#root_formData__title) ').last().find('label').should('not.exist')
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.checkAccessibility()
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.MQTT_DISCONNECT}`)
    })
  })

  describe(OperationData.Function.MQTT_DROP, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.OPERATION,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.MQTT_DROP,
        formData: {},
      },
    }

    it('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.get('h2').should('contain.text', 'Mqtt.drop')
      cy.get('label#root_formData_reasonString-label').should('contain.text', 'Reason String')
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.checkAccessibility()
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.MQTT_DROP}`)
    })
  })

  describe(OperationData.Function.DATAHUB_TRANSFORM, () => {
    const node: Node<OperationData> = {
      id: 'my-node',
      type: DataHubNodeType.OPERATION,
      position: { x: 0, y: 0 },
      data: {
        id: 'default-id',
        functionId: OperationData.Function.DATAHUB_TRANSFORM,
        formData: { transform: ['script-1', 'script-2'] },
      },
    }

    const script1: Node<FunctionData> = {
      id: 'script-1',
      type: DataHubNodeType.FUNCTION,
      position: { x: 0, y: 0 },
      data: { name: 'my function 1', type: 'Javascript', version: 1 },
    }

    const script2: Node<FunctionData> = {
      id: 'script-2',
      type: DataHubNodeType.FUNCTION,
      position: { x: 0, y: 0 },
      data: { name: 'my function 2', type: 'Javascript', version: 1 },
    }

    it('should render the form', () => {
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith(
          [script1, script2, node],
          [
            { id: 'e1', source: 'script-1', target: 'my-node' },
            { id: 'e2', source: 'script-2', target: 'my-node' },
          ]
        ),
      })
      cy.get('h2').first().should('contain.text', 'Transformation')
      cy.get('h2').eq(1).should('contain.text', 'Execution order')
      cy.get('label#root_formData_transform_0-label').should('contain.text', 'Function name')
      cy.get('label#root_formData_transform_0-label + input').should('contain.value', 'my function 1')
      cy.get('label#root_formData_transform_1-label').should('contain.text', 'Function name')
      cy.get('label#root_formData_transform_1-label + input').should('contain.value', 'my function 2')
    })

    it('should be accessible', () => {
      cy.injectAxe()
      cy.mountWithProviders(<OperationPanel selectedNode="my-node" />, {
        wrapper: getWrapperWith([node]),
      })
      cy.checkAccessibility()
      cy.percySnapshot(`Component: OperationPanel > ${OperationData.Function.DATAHUB_TRANSFORM}`)
    })
  })
})
