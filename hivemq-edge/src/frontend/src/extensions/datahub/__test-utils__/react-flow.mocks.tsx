import { Edge, Node } from 'reactflow'
import { DataPolicyValidator } from '@/api/__generated__'
import {
  BehaviorPolicyData,
  BehaviorPolicyType,
  ClientFilterData,
  DataHubNodeType,
  DataPolicyData,
  OperationData,
  SchemaData,
  SchemaType,
  StrategyType,
  TopicFilterData,
  TransitionData,
  TransitionType,
  ValidatorData,
} from '@/extensions/datahub/types.ts'
import { styleDefaultEdge } from '@/extensions/datahub/utils/edge.utils.ts'

export const MOCK_INITIAL_POLICY = () => {
  const baseNode: Node<{ label: string }> = {
    id: '0',
    data: { label: 'A node with 2 handles' },
    position: { x: 0, y: 360 },
    type: 'baseNode',
  }
  const clientNode: Node<ClientFilterData> = {
    id: '1',
    data: { clients: ['client1', 'client2'] },
    position: { x: 15, y: 210 },
    type: DataHubNodeType.CLIENT_FILTER,
  }

  const topicNode: Node<TopicFilterData> = {
    id: '2',
    data: { topics: ['#'] },
    type: DataHubNodeType.TOPIC_FILTER,
    position: { x: 45, y: 120 },
  }

  const dataPolicyNode: Node<DataPolicyData> = {
    id: '3',
    data: {},
    type: DataHubNodeType.DATA_POLICY,
    position: { x: 345, y: 105 },
  }
  const validatorNode: Node<ValidatorData> = {
    id: '4',
    data: {
      strategy: StrategyType.ANY_OF,
      type: DataPolicyValidator.type.SCHEMA,
      schemas: [{ version: '1', schemaId: 'first mock schema' }],
    },
    type: DataHubNodeType.VALIDATOR,
    position: { x: 345, y: -30 },
  }

  const schemaNode: Node<SchemaData> = {
    id: '5',
    data: { name: '5', type: SchemaType.JSON, schemaSource: '', version: 1 },
    type: DataHubNodeType.SCHEMA,
    position: { x: 645, y: -195 },
  }

  const operationNode1: Node<OperationData> = {
    id: '6',
    data: { functionId: '< not set >', metadata: { hasArguments: true } },
    type: DataHubNodeType.OPERATION,
    position: { x: 945, y: 105 },
  }

  const operationNode2: Node<OperationData> = {
    id: '6b',
    data: { functionId: '< not set >', metadata: { isTerminal: true } },
    type: DataHubNodeType.OPERATION,
    position: { x: 1215, y: 105 },
  }

  const behaviorPolicyNode: Node<BehaviorPolicyData> = {
    id: '7',
    data: { model: BehaviorPolicyType.MQTT_EVENT },
    type: DataHubNodeType.BEHAVIOR_POLICY,
    position: { x: 345, y: 195 },
  }

  const transitionNode: Node<TransitionData> = {
    id: '8',
    data: { event: TransitionType.ON_ANY },
    type: DataHubNodeType.TRANSITION,
    position: { x: 645, y: 210 },
  }

  const nodes: Node[] = [
    baseNode,
    clientNode,
    topicNode,
    dataPolicyNode,
    validatorNode,
    schemaNode,
    operationNode1,
    operationNode2,
    behaviorPolicyNode,
    transitionNode,
  ]
  const edges: Edge[] = [
    {
      id: '23',
      source: '2',
      target: '3',
      targetHandle: DataPolicyData.Handle.TOPIC_FILTER,
    },
    {
      id: '36',
      source: '3',
      sourceHandle: DataPolicyData.Handle.ON_SUCCESS,
      target: '6',
    },
    {
      id: '36b',
      source: '3',
      sourceHandle: DataPolicyData.Handle.ON_ERROR,
      target: '6',
    },
    {
      id: '66b',
      source: '6',
      target: '6b',
    },
    {
      id: '34',
      source: '4',
      target: '3',
      targetHandle: DataPolicyData.Handle.VALIDATION,
    },
    {
      id: '54',
      source: '5',
      target: '4',
    },
    {
      id: '17',
      source: '1',
      target: '7',
      targetHandle: BehaviorPolicyData.Handle.CLIENT_FILTER,
    },
    {
      id: '78',
      source: '7',
      target: '8',
    },
    {
      id: '86',
      source: '8',
      target: '6',
    },
    {
      id: '55',
      source: '5',
      target: '6',
      targetHandle: OperationData.Handle.SCHEMA,
    },
  ]

  return { nodes, edges: edges.map((e) => ({ ...e, ...styleDefaultEdge })) }
}
