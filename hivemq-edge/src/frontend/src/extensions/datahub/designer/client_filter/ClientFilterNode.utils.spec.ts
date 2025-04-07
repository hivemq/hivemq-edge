import { expect } from 'vitest'
import type { Connection, Node, NodeAddChange } from '@xyflow/react'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { BehaviorPolicyData, ClientFilterData, WorkspaceState } from '@datahub/types.ts'
import { BehaviorPolicyType, DataHubNodeType } from '@datahub/types.ts'
import { checkValidityClients, loadClientFilter } from '@datahub/designer/client_filter/ClientFilterNode.utils.ts'
import type { BehaviorPolicy } from '@/api/__generated__'

describe('checkValidityClients', () => {
  const MOCK_NODE_BEHAVIOR_POLICY: Node<BehaviorPolicyData> = {
    id: 'node-id',
    type: DataHubNodeType.BEHAVIOR_POLICY,
    data: { id: 'my-policy-id', model: BehaviorPolicyType.MQTT_EVENT },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should return error if no client filter connected', async () => {
    const MOCK_STORE: WorkspaceState = {
      nodes: [],
      edges: [],
      functions: [],
    }

    const { node, data, error, resources } = checkValidityClients(MOCK_NODE_BEHAVIOR_POLICY, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_BEHAVIOR_POLICY)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'No Client Filter connected to Behavior Policy',
        id: 'node-id',
        title: 'BEHAVIOR_POLICY',
        type: 'datahub.notConnected',
      })
    )
  })

  it('should return error if too many client connected', async () => {
    const MOCK_NODE_CLIENT: Node<ClientFilterData> = {
      id: 'node-topic1',
      type: DataHubNodeType.CLIENT_FILTER,
      data: {
        clients: ['client 1'],
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_NODE_CLIENT_2: Node<ClientFilterData> = {
      id: 'node-topic2',
      type: DataHubNodeType.CLIENT_FILTER,
      data: {
        clients: ['client 2'],
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_CLIENT_2, MOCK_NODE_CLIENT, MOCK_NODE_BEHAVIOR_POLICY],
      edges: [
        {
          source: MOCK_NODE_CLIENT.id,
          sourceHandle: 'client 1-0',
          target: MOCK_NODE_BEHAVIOR_POLICY.id,
          targetHandle: 'topicFilter',
          id: '1',
        },

        {
          source: MOCK_NODE_CLIENT_2.id,
          sourceHandle: 'client 2-0',
          target: MOCK_NODE_BEHAVIOR_POLICY.id,
          targetHandle: 'topicFilter',
          id: '2',
        },
      ],
      functions: [],
    }

    const { node, data, error, resources } = checkValidityClients(MOCK_NODE_BEHAVIOR_POLICY, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_CLIENT_2)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'Too many Client Filter connected to Behavior Policy',
        id: 'node-id',
        title: 'BEHAVIOR_POLICY',
        type: 'datahub.cardinality',
      })
    )
  })

  it('should return the payload otherwise', async () => {
    const MOCK_NODE_CLIENT: Node<ClientFilterData> = {
      id: 'node-topic1',
      type: DataHubNodeType.CLIENT_FILTER,
      data: {
        clients: ['client 1'],
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_CLIENT, MOCK_NODE_BEHAVIOR_POLICY],
      edges: [
        {
          source: MOCK_NODE_CLIENT.id,
          sourceHandle: 'client 1-0',
          target: MOCK_NODE_BEHAVIOR_POLICY.id,
          targetHandle: 'topicFilter',
          id: '1',
        },
      ],
      functions: [],
    }

    const { node, data, error, resources } = checkValidityClients(MOCK_NODE_BEHAVIOR_POLICY, MOCK_STORE)
    expect(node).toStrictEqual(MOCK_NODE_CLIENT)
    expect(resources).toBeUndefined()
    expect(data).toEqual('client 1')
    expect(error).toBeUndefined()
  })
})

describe('loadClientFilter', () => {
  const MOCK_NODE_BEHAVIOR: Node<BehaviorPolicyData> = {
    id: 'node-id',
    type: DataHubNodeType.BEHAVIOR_POLICY,
    data: { id: 'my-policy-id', model: BehaviorPolicyType.MQTT_EVENT },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  const behaviorPolicy: BehaviorPolicy = {
    behavior: { id: 'Mqtt.events' },

    id: 'string',
    matching: { clientIdRegex: '*.*' },
  }

  it('should return nodes', () => {
    expect(loadClientFilter(behaviorPolicy, MOCK_NODE_BEHAVIOR)).toStrictEqual<(NodeAddChange | Connection)[]>([
      expect.objectContaining<NodeAddChange>({
        item: {
          data: {
            clients: ['*.*'],
          },
          id: expect.stringContaining('node_'),
          position: {
            x: -320,
            y: 0,
          },
          type: DataHubNodeType.CLIENT_FILTER,
        },
        type: 'add',
      }),
      expect.objectContaining({
        source: expect.stringContaining('node_'),
        target: 'node-id',
      }),
    ])
  })
})
