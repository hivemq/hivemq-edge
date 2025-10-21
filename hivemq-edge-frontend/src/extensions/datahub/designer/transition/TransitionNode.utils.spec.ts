import { expect } from 'vitest'
import type { Connection, Node, NodeAddChange } from '@xyflow/react'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import type { BehaviorPolicyData, OperationData, TransitionData, WorkspaceState } from '@datahub/types.ts'
import { BehaviorPolicyType, DataHubNodeType, StateType } from '@datahub/types.ts'
import {
  checkValidityTransitions,
  extractEventStates,
  getActiveTransition,
  loadTransitions,
} from '@datahub/designer/transition/TransitionNode.utils.ts'
import type { BehaviorPolicyOnTransition, PolicySchema, Script } from '@/api/__generated__'
import { BehaviorPolicyTransitionEvent } from '@/api/__generated__'
import { type BehaviorPolicy } from '@/api/__generated__'

const MOCK_NODE_BEHAVIOR: Node<BehaviorPolicyData> = {
  id: 'node-id',
  type: DataHubNodeType.BEHAVIOR_POLICY,
  data: { id: 'my-policy-id', model: BehaviorPolicyType.MQTT_EVENT },
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
}

describe('checkValidityTransitions', () => {
  it('should return an error if transition not connected', async () => {
    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_BEHAVIOR],
      edges: [],
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
        event: BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
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
          data: { id: 'my-operation-id1' },
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
            id: 'my-operation-id2',
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
    }

    const { behaviorPolicyTransitions, pipelines } = checkValidityTransitions(MOCK_NODE_BEHAVIOR, MOCK_STORE)
    expect(behaviorPolicyTransitions).toHaveLength(1)
    expect(pipelines).toHaveLength(2)

    const [{ node, data, error, resources }] = behaviorPolicyTransitions
    expect(data).toEqual({
      'Mqtt.OnInboundDisconnect': {
        pipeline: [
          {
            arguments: {
              level: 'DEBUG',
              message: 'the message',
            },
            functionId: 'my-function',
            id: 'my-operation-id2',
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
        event: BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
      },
      ...MOCK_DEFAULT_NODE,
      position: { x: 0, y: 0 },
    }
    const MOCK_STORE: WorkspaceState = {
      nodes: [MOCK_NODE_BEHAVIOR, MOCK_NODE_TRANSITION],
      edges: [{ id: '1', source: MOCK_NODE_BEHAVIOR.id, target: MOCK_NODE_TRANSITION.id }],
    }

    const { behaviorPolicyTransitions, pipelines } = checkValidityTransitions(MOCK_NODE_BEHAVIOR, MOCK_STORE)
    expect(pipelines).toStrictEqual([])
    expect(behaviorPolicyTransitions).toHaveLength(1)

    const [{ node, data, error, resources }] = behaviorPolicyTransitions
    expect(data).toEqual({
      'Mqtt.OnInboundSubscribe': {
        pipeline: [],
      },
      fromState: 'NotDuplicated',
      toState: 'Violated',
    })
    expect(resources).toBeUndefined()
    expect(node).toStrictEqual(MOCK_NODE_TRANSITION)
    expect(error).toBeUndefined()
  })
})

describe('getActiveTransition', () => {
  const base: BehaviorPolicyOnTransition = { fromState: 'A', toState: 'B' }
  it.each<[Partial<BehaviorPolicyOnTransition>, string | undefined]>([
    [{}, undefined],
    [{ 'Event.OnAny': { pipeline: [] }, 'Connection.OnDisconnect': { pipeline: [] } }, 'Event.OnAny'],
    [
      { 'Connection.OnDisconnect': { pipeline: [] }, 'Mqtt.OnInboundConnect': { pipeline: [] } },
      'Connection.OnDisconnect',
    ],
    [
      { 'Mqtt.OnInboundConnect': { pipeline: [] }, 'Mqtt.OnInboundDisconnect': { pipeline: [] } },
      'Mqtt.OnInboundConnect',
    ],
    [
      { 'Mqtt.OnInboundDisconnect': { pipeline: [] }, 'Mqtt.OnInboundPublish': { pipeline: [] } },
      'Mqtt.OnInboundDisconnect',
    ],
    [
      { 'Mqtt.OnInboundPublish': { pipeline: [] }, 'Mqtt.OnInboundSubscribe': { pipeline: [] } },
      'Mqtt.OnInboundPublish',
    ],
    [{ 'Mqtt.OnInboundSubscribe': { pipeline: [] } }, 'Mqtt.OnInboundSubscribe'],
  ])('should identify %s as %s', (transition, key) => {
    expect(getActiveTransition({ ...base, ...transition })).toBe(key)
  })
})

describe('extractEventStates', () => {
  const base: BehaviorPolicyOnTransition = { fromState: 'A', toState: 'B' }

  it.each<[BehaviorPolicyType, Partial<BehaviorPolicyOnTransition>, TransitionData]>([
    [BehaviorPolicyType.MQTT_EVENT, {}, { model: BehaviorPolicyType.MQTT_EVENT }],
    [
      BehaviorPolicyType.MQTT_EVENT,
      { 'Connection.OnDisconnect': { pipeline: [] } },
      {
        event: BehaviorPolicyTransitionEvent.CONNECTION_ON_DISCONNECT,
        model: BehaviorPolicyType.MQTT_EVENT,
      },
    ],
  ])('should identify %s %s as %s', (model, transition, res) => {
    expect(extractEventStates(model, { ...base, ...transition })).toEqual(expect.objectContaining(res))
  })
})

describe('loadTransitions', () => {
  const MOCK_NODE_BEHAVIOR: Node<BehaviorPolicyData> = {
    id: 'node-id',
    type: DataHubNodeType.BEHAVIOR_POLICY,
    data: { id: 'my-policy-id', model: BehaviorPolicyType.MQTT_EVENT },
    ...MOCK_DEFAULT_NODE,
    position: { x: 0, y: 0 },
  }

  it('should return nodes', () => {
    const behaviorPolicy: BehaviorPolicy = {
      behavior: { id: 'Mqtt.events' },

      id: 'string',
      matching: { clientIdRegex: '*.*' },
    }
    const schemas: PolicySchema[] = []
    const scripts: Script[] = []

    expect(loadTransitions(behaviorPolicy, schemas, scripts, MOCK_NODE_BEHAVIOR)).toStrictEqual<
      (NodeAddChange | Connection)[]
    >([])
  })

  it('should throw error', () => {
    const behaviorPolicy: BehaviorPolicy = {
      behavior: { id: 'FAKE_MODEL' },
      id: 'string',
      matching: { clientIdRegex: '*.*' },
    }
    const schemas: PolicySchema[] = []
    const scripts: Script[] = []
    expect(() => loadTransitions(behaviorPolicy, schemas, scripts, MOCK_NODE_BEHAVIOR)).toThrow(
      'Something is wrong with the transition model'
    )
  })

  it('should load transitions with multiple onTransitions', () => {
    const behaviorPolicy: BehaviorPolicy = {
      behavior: { id: 'Mqtt.events' },
      id: 'my-policy-id',
      matching: { clientIdRegex: '*.*' },
      onTransitions: [
        {
          fromState: 'NotDuplicated',
          toState: 'Violated',
          'Mqtt.OnInboundPublish': {
            pipeline: [
              {
                id: 'operation-1',
                functionId: 'System.log',
                arguments: { message: 'test' },
              },
            ],
          },
        },
        {
          fromState: 'Violated',
          toState: 'NotDuplicated',
          'Mqtt.OnInboundDisconnect': {
            pipeline: [],
          },
        },
      ],
    }
    const schemas: PolicySchema[] = []
    const scripts: Script[] = []

    const result = loadTransitions(behaviorPolicy, schemas, scripts, MOCK_NODE_BEHAVIOR)

    // Should have 2 transitions + 2 connections = 4 items minimum
    expect(result.length).toBeGreaterThanOrEqual(4)

    // Check that transitions are created
    const transitionNodes = result.filter((item) => 'item' in item && item.item.type === DataHubNodeType.TRANSITION)
    expect(transitionNodes).toHaveLength(2)

    // Verify first transition data
    const firstTransition = transitionNodes[0] as NodeAddChange
    expect(firstTransition.item.data).toEqual(
      expect.objectContaining({
        model: BehaviorPolicyType.MQTT_EVENT,
        from: StateType.NotDuplicated,
        to: StateType.Violated,
        event: BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
      })
    )

    // Verify second transition data
    const secondTransition = transitionNodes[1] as NodeAddChange
    expect(secondTransition.item.data).toEqual(
      expect.objectContaining({
        model: BehaviorPolicyType.MQTT_EVENT,
        from: StateType.Violated,
        to: StateType.NotDuplicated,
        event: BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
      })
    )

    // Verify connections exist
    const connections = result.filter((item) => 'source' in item && 'target' in item)
    expect(connections.length).toBeGreaterThanOrEqual(2)
  })

  it('should adjust positions for multiple transitions', () => {
    const behaviorPolicy: BehaviorPolicy = {
      behavior: { id: 'Mqtt.events' },
      id: 'my-policy-id',
      matching: { clientIdRegex: '*.*' },
      onTransitions: [
        {
          fromState: 'A',
          toState: 'B',
          'Event.OnAny': { pipeline: [] },
        },
        {
          fromState: 'B',
          toState: 'C',
          'Event.OnAny': { pipeline: [] },
        },
        {
          fromState: 'C',
          toState: 'A',
          'Event.OnAny': { pipeline: [] },
        },
      ],
    }
    const schemas: PolicySchema[] = []
    const scripts: Script[] = []

    const result = loadTransitions(behaviorPolicy, schemas, scripts, MOCK_NODE_BEHAVIOR)
    const transitionNodes = result.filter((item) => 'item' in item && item.item.type === DataHubNodeType.TRANSITION)

    expect(transitionNodes).toHaveLength(3)

    // Verify that positions are different for each transition
    const positions = transitionNodes.map((node) => (node as NodeAddChange).item.position.y)
    const uniquePositions = new Set(positions)
    expect(uniquePositions.size).toBe(3)
  })
})
