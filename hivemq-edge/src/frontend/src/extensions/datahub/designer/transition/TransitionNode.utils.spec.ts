import { expect } from 'vitest'
import { Node } from 'reactflow'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import {
  BehaviorPolicyData,
  BehaviorPolicyType,
  DataHubNodeType,
  OperationData,
  StateType,
  TransitionData,
  TransitionType,
  WorkspaceState,
} from '@datahub/types.ts'
import { checkValidityTransitions } from '@datahub/designer/transition/TransitionNode.utils.ts'

const MOCK_NODE_BEHAVIOR: Node<BehaviorPolicyData> = {
  id: 'node-id',
  type: DataHubNodeType.BEHAVIOR_POLICY,
  data: {
    model: BehaviorPolicyType.MQTT_EVENT,
  },
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

describe('checkValidityTransitions', () => {
  it('should return an error if transition not connected', async () => {
    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_BEHAVIOR],
      edges: [],
      functions: [],
    }

    const { behaviorPolicyTransitions, pipelines } = checkValidityTransitions(MOCK_NODE_BEHAVIOR, MOCK_STORE)
    expect(pipelines).toBeUndefined()
    expect(behaviorPolicyTransitions).toHaveLength(1)

    const [{ node, data, error, resources }] = behaviorPolicyTransitions
    expect(data).toBeUndefined()
    expect(resources).toBeUndefined()
    expect(node).toStrictEqual(MOCK_NODE_BEHAVIOR)
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'No Transition connected to Behavior Policy',
        id: MOCK_NODE_BEHAVIOR.id,
        title: DataHubNodeType.BEHAVIOR_POLICY,
        type: 'datahub.notConnected',
      })
    )
  })

  it('should return an error if transition not configure', async () => {
    const MOCK_NODE_TRANSITION: Node<TransitionData> = {
      id: 'node-id',
      type: DataHubNodeType.TRANSITION,
      data: {},
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_BEHAVIOR, MOCK_NODE_TRANSITION],
      edges: [{ id: '1', source: MOCK_NODE_BEHAVIOR.id, target: MOCK_NODE_TRANSITION.id }],
      functions: [],
    }

    const { behaviorPolicyTransitions, pipelines } = checkValidityTransitions(MOCK_NODE_BEHAVIOR, MOCK_STORE)
    expect(pipelines).toStrictEqual([])
    expect(behaviorPolicyTransitions).toHaveLength(1)

    const [{ node, data, error, resources }] = behaviorPolicyTransitions
    expect(data).toBeUndefined()
    expect(resources).toBeUndefined()
    expect(node).toStrictEqual(MOCK_NODE_TRANSITION)
    expect(error).toEqual(
      expect.objectContaining({
        detail: 'The Transition is not properly defined. The following properties are missing: event, from, to',
        id: 'node-id',
        status: 404,
        title: 'TRANSITION',
        type: 'datahub.notConfigured',
      })
    )
  })

  it('should return a valid object with partial pipeline', async () => {
    const MOCK_TRANSITION: Node<TransitionData> = {
      id: 'node_697b',
      type: DataHubNodeType.TRANSITION,
      position: {
        x: 650,
        y: 325,
      },
      data: {
        event: TransitionType.ON_INBOUND_DISCONNECT,
        from: StateType.NotDuplicated,
        to: StateType.Violated,
      },
      width: 235,
      height: 48,
      selected: false,
      positionAbsolute: {
        x: 650,
        y: 325,
      },
    }
    const MOCK_STORE: WorkspaceState = {
      nodes: [
        {
          id: 'node_3e14',
          type: DataHubNodeType.OPERATION,
          position: {
            x: 1275,
            y: 225,
          },
          data: {},
          width: 233,
          height: 56,
        },
        {
          id: 'node_8c4b',
          type: DataHubNodeType.OPERATION,
          position: {
            x: 975,
            y: 275,
          },
          data: {
            functionId: 'my-function',
            formData: {
              level: 'DEBUG',
              message: 'the message',
            },
          },
          width: 233,
          height: 56,
          selected: false,
          positionAbsolute: {
            x: 975,
            y: 275,
          },
          dragging: false,
        } as Node<OperationData>,
        MOCK_TRANSITION,
        MOCK_NODE_BEHAVIOR,
      ],
      edges: [
        {
          source: MOCK_NODE_BEHAVIOR.id,
          sourceHandle: 'transitions',
          target: 'node_697b',
          targetHandle: 'target',
          id: '1',
        },
        {
          source: 'node_697b',
          sourceHandle: 'source',
          target: 'node_8c4b',
          targetHandle: 'input',
          id: '2',
        },
        {
          source: 'node_8c4b',
          sourceHandle: 'output',
          target: 'node_3e14',
          targetHandle: 'input',
          id: '3',
        },
      ],
      functions: [],
    }

    const { behaviorPolicyTransitions, pipelines } = checkValidityTransitions(MOCK_NODE_BEHAVIOR, MOCK_STORE)
    expect(behaviorPolicyTransitions).toHaveLength(1)
    expect(pipelines).toHaveLength(2)

    const [{ node, data, error, resources }] = behaviorPolicyTransitions
    expect(data).toEqual({
      'Mqtt.OnInboundDisconnect': {
        pipelines: [
          {
            arguments: {
              level: 'DEBUG',
              message: 'the message',
            },
            functionId: 'my-function',
            id: 'node_8c4b',
          },
        ],
      },
      fromState: 'NotDuplicated',
      toState: 'Violated',
    })
    expect(resources).toBeUndefined()
    expect(node).toStrictEqual(MOCK_TRANSITION)
    expect(error).toBeUndefined()
  })

  it('should return a valid object otherwise', async () => {
    const MOCK_NODE_TRANSITION: Node<TransitionData> = {
      id: 'node-id',
      type: DataHubNodeType.TRANSITION,
      data: {
        to: StateType.Violated,
        from: StateType.NotDuplicated,
        event: TransitionType.ON_INBOUND_SUBSCRIBE,
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_BEHAVIOR, MOCK_NODE_TRANSITION],
      edges: [{ id: '1', source: MOCK_NODE_BEHAVIOR.id, target: MOCK_NODE_TRANSITION.id }],
      functions: [],
    }

    const { behaviorPolicyTransitions, pipelines } = checkValidityTransitions(MOCK_NODE_BEHAVIOR, MOCK_STORE)
    expect(pipelines).toStrictEqual([])
    expect(behaviorPolicyTransitions).toHaveLength(1)

    const [{ node, data, error, resources }] = behaviorPolicyTransitions
    expect(data).toEqual({
      'Mqtt.OnInboundSubscribe': {
        pipelines: [],
      },
      fromState: 'NotDuplicated',
      toState: 'Violated',
    })
    expect(resources).toBeUndefined()
    expect(node).toStrictEqual(MOCK_NODE_TRANSITION)
    expect(error).toBeUndefined()
  })
})
