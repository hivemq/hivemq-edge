import { expect } from 'vitest'
import { Node } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { BehaviorPolicyData, BehaviorPolicyType, DataHubNodeType } from '@datahub/types.ts'
import { checkValidityModel } from '@datahub/designer/behavior_policy/BehaviorPolicyNode.utils.ts'

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
      data: { model: BehaviorPolicyType.PUBLISH_DUPLICATE },
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
