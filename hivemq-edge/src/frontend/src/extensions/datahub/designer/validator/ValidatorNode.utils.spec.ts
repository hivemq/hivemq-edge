import { expect } from 'vitest'
import { Node } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import {
  DataHubNodeType,
  DataPolicyData,
  SchemaData,
  SchemaType,
  StrategyType,
  ValidatorData,
  ValidatorType,
  WorkspaceState,
} from '@datahub/types.ts'
import {
  checkValidityPolicyValidator,
  checkValidityPolicyValidators,
} from '@datahub/designer/validator/ValidatorNode.utils.ts'

const MOCK_NODE_VALIDATOR: Node<ValidatorData> = {
  id: 'node-id',
  type: DataHubNodeType.VALIDATOR,
  data: {
    type: ValidatorType.SCHEMA,
    strategy: StrategyType.ANY_OF,
    schemas: [],
  },
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

describe('checkValidityPolicyValidator', () => {
  it('should return error if not connected', async () => {
    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
      functions: [],
    }

    const { node, data, error, resources } = checkValidityPolicyValidator(MOCK_NODE_VALIDATOR, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_VALIDATOR)
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'No Schema connected to Policy Validator',
        id: 'node-id',
        status: 404,
        title: 'VALIDATOR',
        type: 'datahub.notConnected',
      })
    )
    expect(data).toBeUndefined()
    expect(resources).toBeUndefined()
  })

  it('should return a payload otherwise', async () => {
    const MOCK_NODE_SCHEMA: Node<SchemaData> = {
      id: 'node-schema',
      type: DataHubNodeType.SCHEMA,
      data: {
        type: SchemaType.JSON,
        version: '1',
        schemaSource: '{}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_SCHEMA, MOCK_NODE_VALIDATOR],
      edges: [{ id: '1', source: MOCK_NODE_SCHEMA.id, target: MOCK_NODE_VALIDATOR.id }],
      functions: [],
    }

    const { node, data, error, resources } = checkValidityPolicyValidator(MOCK_NODE_VALIDATOR, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_VALIDATOR)
    expect(data).toEqual(
      expect.objectContaining({
        arguments: [
          {
            schemaId: 'node-schema',
            version: '1',
          },
        ],
        type: 'schema',
      })
    )
    expect(error).toBeUndefined()
    expect(resources).toHaveLength(1)
    expect(resources?.[0]).toStrictEqual(
      expect.objectContaining({
        data: {
          arguments: {},
          id: 'node-schema',
          schemaDefinition: 'Int9Ig==',
          type: 'JSON',
          version: '1',
        },
        node: MOCK_NODE_SCHEMA,
      })
    )
  })
})

describe('checkValidityPolicyValidators', () => {
  const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
    id: 'node-policy',
    type: DataHubNodeType.DATA_POLICY,
    data: {},
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should return error if not connected', async () => {
    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
      functions: [],
    }

    const results = checkValidityPolicyValidators(MOCK_NODE_DATA_POLICY, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node, data, error, resources } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_DATA_POLICY)
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'No Policy Validator connected to Data Policy',
        id: 'node-policy',
        status: 404,
        title: 'DATA_POLICY',
        type: 'datahub.notConnected',
      })
    )
    expect(data).toBeUndefined()
    expect(resources).toBeUndefined()
  })

  it('should return a payload otherwise', async () => {
    const MOCK_NODE_SCHEMA: Node<SchemaData> = {
      id: 'node-schema',
      type: DataHubNodeType.SCHEMA,
      data: {
        type: SchemaType.JSON,
        version: '1',
        schemaSource: '{}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_SCHEMA, MOCK_NODE_VALIDATOR, MOCK_NODE_DATA_POLICY],
      edges: [
        { id: '1', source: MOCK_NODE_SCHEMA.id, target: MOCK_NODE_VALIDATOR.id },
        { id: '2', source: MOCK_NODE_VALIDATOR.id, target: MOCK_NODE_DATA_POLICY.id },
      ],
      functions: [],
    }

    const results = checkValidityPolicyValidators(MOCK_NODE_DATA_POLICY, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_VALIDATOR)
  })
})
