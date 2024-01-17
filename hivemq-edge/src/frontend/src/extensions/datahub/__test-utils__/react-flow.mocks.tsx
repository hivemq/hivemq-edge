import { Edge, Node } from 'reactflow'
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
  ValidatorType,
} from '@/extensions/datahub/types.ts'

export const MOCK_INITIAL_POLICY = () => {
  const baseNode: Node<{ label: string }> = {
    id: '0',
    data: { label: 'A node with 2 handles' },
    position: { x: 0, y: -200 },
    type: 'baseNode',
  }
  const clientNode: Node<ClientFilterData> = {
    id: '1',
    data: { clients: ['client1', 'client2'] },
    position: { x: 0, y: 300 },
    type: DataHubNodeType.CLIENT_FILTER,
  }

  const topicNode: Node<TopicFilterData> = {
    id: '2',
    data: { topics: ['#'] },
    type: DataHubNodeType.TOPIC_FILTER,
    position: { x: 0, y: 100 },
  }

  const dataPolicyNode: Node<DataPolicyData> = {
    id: '3',
    data: {},
    type: DataHubNodeType.DATA_POLICY,
    position: { x: 400, y: 100 },
  }
  const validatorNode: Node<ValidatorData> = {
    id: '4',
    data: { strategy: StrategyType.ANY_OF, type: ValidatorType.SCHEMA },
    type: DataHubNodeType.VALIDATOR,
    position: { x: 400, y: 0 },
  }

  const schemaNode: Node<SchemaData> = {
    id: '5',
    data: { type: SchemaType.JSON, schemaSource: { title: '< untitled >' } },
    type: DataHubNodeType.SCHEMA,
    position: { x: 400, y: -100 },
  }

  const opera: Node<OperationData> = {
    id: '6',
    data: { action: { functionId: '< not set >', hasArguments: true } },
    type: DataHubNodeType.OPERATION,
    position: { x: 600, y: 100 },
  }

  const behaviorPolicyNode: Node<BehaviorPolicyData> = {
    id: '7',
    data: { model: BehaviorPolicyType.MQTT_EVENT },
    type: DataHubNodeType.BEHAVIOR_POLICY,
    position: { x: 400, y: 300 },
  }

  const transitionNode: Node<TransitionData> = {
    id: '8',
    data: { type: TransitionType.ON_ANY },
    type: DataHubNodeType.TRANSITION,
    position: { x: 600, y: 300 },
  }

  const nodes: Node[] = [
    baseNode,
    clientNode,
    topicNode,
    dataPolicyNode,
    validatorNode,
    schemaNode,
    opera,
    behaviorPolicyNode,
    transitionNode,
  ]
  const edges: Edge[] = []

  return { nodes, edges }
}
