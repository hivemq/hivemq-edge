import { expect } from 'vitest'
import type { Connection, Node, NodeAddChange } from '@xyflow/react'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { DataPolicy, PolicySchema } from '@/api/__generated__'
import { DataPolicyValidator } from '@/api/__generated__'
import { DataHubNodeType, SchemaType, StrategyType } from '@datahub/types.ts'
import type { DataPolicyData, SchemaData, ValidatorData, WorkspaceState } from '@datahub/types.ts'

import {
  checkValidityPolicyValidator,
  checkValidityPolicyValidators,
  loadValidators,
} from '@datahub/designer/validator/ValidatorNode.utils.ts'

const NODE_SCHEMA_ID = 'node-schema'

const MOCK_NODE_VALIDATOR: Node<ValidatorData> = {
  id: 'node-id',
  type: DataHubNodeType.VALIDATOR,
  data: {
    type: DataPolicyValidator.type.SCHEMA,
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
      id: NODE_SCHEMA_ID,
      type: DataHubNodeType.SCHEMA,
      data: {
        name: NODE_SCHEMA_ID,
        type: SchemaType.JSON,
        version: 1,
        schemaSource: '{}',
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_SCHEMA, MOCK_NODE_VALIDATOR],
      edges: [{ id: '1', source: MOCK_NODE_SCHEMA.id, target: MOCK_NODE_VALIDATOR.id }],
    }

    const { node, data, error, resources } = checkValidityPolicyValidator(MOCK_NODE_VALIDATOR, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_VALIDATOR)
    expect(data).toEqual(
      expect.objectContaining({
        arguments: {
          schemas: [
            {
              schemaId: NODE_SCHEMA_ID,
              version: '1',
            },
          ],
          strategy: 'ANY_OF',
        },
        type: 'SCHEMA',
      })
    )
    expect(error).toBeUndefined()
    expect(resources).toHaveLength(1)
    expect(resources?.[0]).toStrictEqual(
      expect.objectContaining({
        data: {
          id: NODE_SCHEMA_ID,
          schemaDefinition: 'e30=',
          type: 'JSON',
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
    data: { id: 'my-policy-id' },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should return a payload otherwise', async () => {
    const MOCK_NODE_SCHEMA: Node<SchemaData> = {
      id: NODE_SCHEMA_ID,
      type: DataHubNodeType.SCHEMA,
      data: {
        name: NODE_SCHEMA_ID,
        type: SchemaType.JSON,
        version: 1,
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
    }

    const results = checkValidityPolicyValidators(MOCK_NODE_DATA_POLICY, MOCK_STORE)
    expect(results).toHaveLength(1)
    const { node } = results[0]
    expect(node).toStrictEqual(MOCK_NODE_VALIDATOR)
  })
})

describe('loadValidators', () => {
  const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
    id: 'node-id',
    type: DataHubNodeType.DATA_POLICY,
    data: { id: 'my-policy-id' },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  const schemas: PolicySchema[] = [
    {
      id: 'test',
      version: 1,
      type: 'JSON',
      schemaDefinition:
        'ewogICAiJHNjaGVtYSI6Imh0dHBzOi8vanNvbi1zY2hlbWEub3JnL2RyYWZ0LzIwMjAtMTIvc2NoZW1hIiwKICAgInRpdGxlIjoiZGZkZmRmZGYiLAogICAiZGVzY3JpcHRpb24iOiIiLAogICAicmVxdWlyZWQiOltdLAogICAidHlwZSI6Im9iamVjdCIsCiAgICJwcm9wZXJ0aWVzIjp7fQp9Cg==',
    },
  ]

  it('should return nodes', () => {
    const dataPolicy: DataPolicy = {
      id: 'policy1',
      matching: {
        topicFilter: 'topic/example/2',
      },
      validation: {
        validators: [
          {
            type: DataPolicyValidator.type.SCHEMA,
            arguments: {
              strategy: 'ALL_OF',
              schemas: [
                {
                  schemaId: 'test',
                  version: '1',
                },
                {
                  schemaId: 'test',
                  version: '1',
                },
              ],
            },
          },
        ],
      },
    }

    expect(loadValidators(dataPolicy, schemas, MOCK_NODE_DATA_POLICY)).toStrictEqual<(NodeAddChange | Connection)[]>([
      expect.objectContaining({
        item: {
          data: {
            schemas: [
              {
                schemaId: 'test',
                version: '1',
              },
              {
                schemaId: 'test',
                version: '1',
              },
            ],
            strategy: 'ALL_OF',
            type: 'SCHEMA',
          },
          id: expect.stringContaining('VALIDATOR_'),
          position: {
            x: -320,
            y: 160,
          },
          type: 'VALIDATOR',
        },
        type: 'add',
      }),
      expect.objectContaining({
        source: expect.stringContaining('VALIDATOR_'),
        target: 'node-id',
        targetHandle: 'validation',
      }),
      expect.objectContaining({
        item: {
          data: {
            internalVersions: [1],
            name: 'test',
            schemaSource: expect.stringContaining('dfdfdfdf'),
            type: 'JSON',
            version: 1,
          },
          id: expect.stringContaining('SCHEMA_'),
          position: {
            x: -640,
            y: 160,
          },
          type: 'SCHEMA',
        },
        type: 'add',
      }),
      expect.objectContaining({
        source: expect.stringContaining('SCHEMA_'),
        target: expect.stringContaining('VALIDATOR_'),
      }),
      expect.objectContaining({
        item: {
          data: {
            internalVersions: [1],
            name: 'test',
            schemaSource: expect.stringContaining('dfdfdfdf'),
            type: 'JSON',
            version: 1,
          },
          id: expect.stringContaining('SCHEMA_'),
          position: {
            x: -640,
            y: 320,
          },
          type: 'SCHEMA',
        },
        type: 'add',
      }),
      expect.objectContaining({
        source: expect.stringContaining('SCHEMA_'),
        target: expect.stringContaining('VALIDATOR_'),
      }),
    ])
  })

  it('should be used in the right context', () => {
    const dataPolicy: DataPolicy = {
      id: 'policy1',
      matching: {
        topicFilter: 'topic/example/2',
      },
      validation: {
        validators: [
          {
            // @ts-ignore This should not happen
            type: 'FAKE_VALIDATOR',
            arguments: {
              strategy: 'ALL_OF',
              schemas: [
                {
                  schemaId: 'test',
                  version: '1',
                },
                {
                  schemaId: 'test',
                  version: '1',
                },
              ],
            },
          },
        ],
      },
    }

    expect(() => loadValidators(dataPolicy, schemas, MOCK_NODE_DATA_POLICY)).toThrow(
      'Cannot find the Policy Validator node'
    )
  })
})
