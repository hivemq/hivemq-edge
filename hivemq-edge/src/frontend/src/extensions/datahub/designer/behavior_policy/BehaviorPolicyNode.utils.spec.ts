import { expect } from 'vitest'
import type { Node, NodeAddChange } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { BehaviorPolicyData } from '@datahub/types.ts'
import { BehaviorPolicyType, DataHubNodeType } from '@datahub/types.ts'
import { checkValidityModel, loadBehaviorPolicy } from '@datahub/designer/behavior_policy/BehaviorPolicyNode.utils.ts'
import type { BehaviorPolicy, BehaviorPolicyOnTransition } from '@/api/__generated__'

describe('checkValidityModel', () => {
  it('should return error if no model configured', async () => {
    const MOCK_NODE_BEHAVIOR_POLICY: Node<BehaviorPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.BEHAVIOR_POLICY,
      // @ts-ignore
      data: { model: undefined },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { node, data, error, resources } = checkValidityModel(MOCK_NODE_BEHAVIOR_POLICY)
    expect(node).toStrictEqual(MOCK_NODE_BEHAVIOR_POLICY)
    expect(resources).toBeUndefined()
    expect(data).toBeUndefined()
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'The Behavior Policy is not properly defined. The following properties are missing: model',
        id: 'node-id',
        title: 'BEHAVIOR_POLICY',
        type: 'datahub.notConfigured',
      })
    )
  })

  it('should return the payload otherwise', async () => {
    const MOCK_NODE_BEHAVIOR_POLICY: Node<BehaviorPolicyData> = {
      id: 'node-id',
      type: DataHubNodeType.BEHAVIOR_POLICY,
      data: { id: 'my-policy-id', model: BehaviorPolicyType.PUBLISH_DUPLICATE },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }

    const { node, data, error, resources } = checkValidityModel(MOCK_NODE_BEHAVIOR_POLICY)
    expect(node).toStrictEqual(MOCK_NODE_BEHAVIOR_POLICY)
    expect(resources).toBeUndefined()
    expect(data).toEqual(
      expect.objectContaining({
        arguments: undefined,
        id: 'Publish.duplicate',
      })
    )
    expect(error).toBeUndefined()
  })
})

describe('loadBehaviorPolicy', () => {
  const base: BehaviorPolicyOnTransition = { fromState: 'A', toState: 'B' }

  it('should return nodes', () => {
    const behaviorPolicy: BehaviorPolicy = {
      behavior: { id: 'Mqtt.events' },
      createdAt: '',

      id: 'string',

      lastUpdatedAt: 'string',
      matching: { clientIdRegex: 'ss' },
      onTransitions: [base],
    }
    expect(loadBehaviorPolicy(behaviorPolicy)).toEqual<NodeAddChange>({
      item: {
        data: {
          arguments: undefined,
          id: 'string',
          model: 'Mqtt.events',
        },
        id: expect.stringContaining(''),
        position: {
          x: 0,
          y: 0,
        },
        type: 'BEHAVIOR_POLICY',
      },
      type: 'add',
    })
  })

  it('should throw an error without a model', () => {
    const behaviorPolicy: BehaviorPolicy = {
      behavior: { id: 'fake' },
      id: 'string',
      matching: { clientIdRegex: 'ss' },
    }
    expect(() => loadBehaviorPolicy(behaviorPolicy)).toThrowError('Cannot find the Behavior Policy node')
  })
})
