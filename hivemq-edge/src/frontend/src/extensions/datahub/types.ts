import { Connection, Edge, EdgeAddChange, Node, NodeAddChange, OnEdgesChange, OnNodesChange } from 'reactflow'
import {
  BehaviorPolicy,
  BehaviorPolicyOnTransition,
  DataPolicy,
  DataPolicyValidator,
  PolicyOperation,
  Schema,
} from '@/api/__generated__'
import { RJSFSchema, UiSchema } from '@rjsf/utils'

export interface WorkspaceState {
  nodes: Node[]
  edges: Edge[]
}

export interface WorkspaceAction {
  reset: () => void
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onConnect: (connection: Connection) => void
  onAddNodes: (changes: NodeAddChange[]) => void
  onAddEdges: (changes: EdgeAddChange[]) => void
  onUpdateNodes: <T>(item: string, data: T) => void
}

export enum PolicyType {
  DATA = 'DATA',
  BEHAVIOR = 'BEHAVIOR',
}

export enum DataHubNodeType {
  ADAPTOR = 'ADAPTOR',
  EDGE = 'EDGE',
  BRIDGE = 'BRIDGE',
  TOPIC_FILTER = 'TOPIC_FILTER',
  CLIENT_FILTER = 'CLIENT_FILTER',
  DATA_POLICY = 'DATA_POLICY',
  BEHAVIOR_POLICY = 'BEHAVIOR_POLICY',
  VALIDATOR = 'VALIDATOR',
  SCHEMA = 'SCHEMA',
  OPERATION = 'OPERATION',
  TRANSITION = 'TRANSITION',
  EVENT = 'EVENT',
}

export interface TopicFilterData {
  topics: string[]
}

export interface ClientFilterData {
  clients: string[]
}

export interface DataPolicyData {
  core?: DataPolicy
}

// TODO[18740] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18740/details/
export enum ValidatorType {
  SCHEMA = 'schema',
}

// TODO[18740] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18740/details/
export enum StrategyType {
  ALL_OF = 'ALL_OF',
  ANY_OF = 'ANY_OF',
}

export interface ValidatorData {
  type: ValidatorType
  strategy: StrategyType
  core?: DataPolicyValidator
}

// TODO[18755] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18755/details/
export enum SchemaType {
  JSON = 'JSON',
}

export interface SchemaData {
  type: SchemaType
  schemaSource?: RJSFSchema
  core?: Schema
}

// TODO[18763] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18763/details/
export interface FunctionSpecs {
  functionId: string
  isTerminal?: boolean
  isDataOnly?: boolean
  hasArguments?: boolean
  schema?: RJSFSchema
  uiSchema?: UiSchema
}

export interface OperationData {
  action?: FunctionSpecs
  formData?: Record<string, string | number>
  core?: PolicyOperation
}

// TODO[18757] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18757/details/
export enum BehaviorPolicyType {
  MQTT_EVENT = 'Mqtt.events',
  PUBLISH_DUPLICATE = 'Publish.duplicate',
  PUBLISH_QUOTE = 'Publish.quota',
}

export interface BehaviorPolicyData {
  model: BehaviorPolicyType
  arguments?: Record<string, string | number>
  core?: BehaviorPolicy
}

// TODO[18761] Add to the OpenAPI specs, see https://hivemq.kanbanize.com/ctrl_board/4/cards/18761/details/
export enum StateType {
  Any = 'Any.*',
  Initial = 'Initial',
  Connected = 'Connected',
  Disconnected = 'Disconnected',
  Duplicated = 'Duplicated',
  NotDuplicated = 'NotDuplicated',
  Violated = 'Violated',
  Publishing = 'Publishing',
}

// TODO[18761] Add to the OpenAPI specs, see https://hivemq.kanbanize.com/ctrl_board/4/cards/18761/details/
export enum TransitionType {
  ON_ANY = 'Event.OnAny',
  ON_DISCONNECT = 'Connection.OnDisconnect',
  ON_INBOUND_CONNECT = 'Mqtt.OnInboundConnect',
  ON_INBOUND_DISCONNECT = 'Mqtt.OnInboundDisconnect',
  ON_INBOUND_PUBLISH = 'Mqtt.OnInboundPublish',
  ON_INBOUND_SUBSCRIBE = 'Mqtt.OnInboundSubscribe',
}

export interface TransitionData {
  type?: TransitionType
  from?: StateType
  to?: StateType
  core?: BehaviorPolicyOnTransition
}
