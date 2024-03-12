import { Connection, Edge, EdgeAddChange, Node, NodeAddChange, OnEdgesChange, OnNodesChange } from 'reactflow'
import {
  BehaviorPolicy,
  BehaviorPolicyOnTransition,
  DataPolicy,
  DataPolicyValidator,
  PolicyOperation,
  Schema,
  SchemaReference,
} from '@/api/__generated__'
import { RJSFSchema, UiSchema } from '@rjsf/utils'
import { IChangeEvent } from '@rjsf/core'
import { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'

export interface PanelSpecs {
  schema: RJSFSchema
  uiSchema?: UiSchema
}

export interface PanelProps {
  selectedNode: string
  onFormSubmit?: (data: IChangeEvent) => void
}

export interface WorkspaceState {
  nodes: Node[]
  edges: Edge[]
  functions: FunctionSpecs[]
}

export interface WorkspaceAction {
  reset: () => void
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onConnect: (connection: Connection) => void
  onAddNodes: (changes: NodeAddChange[]) => void
  onAddEdges: (changes: EdgeAddChange[]) => void
  onUpdateNodes: <T>(item: string, data: T) => void

  onAddFunctions: (changes: FunctionSpecs[]) => void
  onSerializePolicy: (node: Node<DataPolicyData | BehaviorPolicyData>) => string | undefined

  isDirty: () => boolean
}

export interface PolicyCheckState {
  node?: Node
  status?: PolicyDryRunStatus
  report?: DryRunResults<unknown, never>[]
}

export interface PolicyCheckAction {
  reset: () => void
  setNode: (node: Node | undefined) => void
  initReport: () => void
  setReport: (report: DryRunResults<unknown, never>[]) => void
  getErrors: () => ProblemDetailsExtended[] | undefined
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
  FUNCTION = 'FUNCTION',
  EVENT = 'EVENT',
}

export enum PolicyType {
  CREATE_POLICY = 'CREATE_POLICY',
  DATA_POLICY = DataHubNodeType.DATA_POLICY,
  BEHAVIOR_POLICY = DataHubNodeType.BEHAVIOR_POLICY,
}

export enum NodeCategory {
  DEFAULT = 'DEFAULT',
  INITIAL = 'INITIAL',
  POLICY = 'POLICY',
  RESOURCE = 'RESOURCE',
}

export interface DataHubNodeData {
  dryRunStatus?: PolicyDryRunStatus
}

export interface TopicFilterData extends DataHubNodeData {
  adapter?: string
  topics: string[]
}

export interface ClientFilterData extends DataHubNodeData {
  clients: string[]
}

export interface DataPolicyData extends DataHubNodeData {
  core?: DataPolicy
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace DataPolicyData {
  export enum Handle {
    TOPIC_FILTER = 'topicFilter',
    VALIDATION = 'validation',
    ON_SUCCESS = 'onSuccess',
    ON_ERROR = 'onError',
  }
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

export interface SchemaArguments {
  schemas: SchemaReference[]
  strategy: StrategyType
}

export interface ValidatorData extends DataHubNodeData {
  type: ValidatorType
  strategy: StrategyType
  schemas: SchemaReference[]
  core?: DataPolicyValidator
}

// TODO[18755] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18755/details/
export enum SchemaType {
  JSON = 'JSON',
  PROTOBUF = 'PROTOBUF',
}

export interface SchemaProtobufArguments {
  messageType: string
}

export interface SchemaData extends DataHubNodeData {
  type: SchemaType
  version: number
  schemaSource?: string
  messageType?: string
  core?: Schema
}

export interface FunctionData extends DataHubNodeData {
  type: 'Javascript'
  name: string
  version: number
  sourceCode?: string
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FunctionData {
  export enum Handle {
    SCHEMA = 'schema',
  }
}

// TODO[18763] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18763/details/
export interface FunctionDefinition {
  isTerminal?: boolean
  isDataOnly?: boolean
  hasArguments?: boolean
}

export interface FunctionSpecs {
  functionId?: string
  metadata?: FunctionDefinition
  schema?: RJSFSchema
  uiSchema?: UiSchema
}

export interface PolicyOperationArguments {
  schemaId: string
  schemaVersion: string
}

export interface OperationData extends DataHubNodeData {
  functionId?: string
  metadata?: FunctionDefinition
  formData?: Record<string, string | number | string[]>
  core?: PolicyOperation
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace OperationData {
  export enum Function {
    SYSTEM_LOG = 'System.log',
    MQTT_USER_PROPERTY = 'Mqtt.UserProperties.add',
    MQTT_DISCONNECT = 'Mqtt.disconnect',
    MQTT_DROP = 'Mqtt.drop',
    DELIVERY_REDIRECT = 'Delivery.redirectTo',
    SERDES_DESERIALIZE = 'Serdes.deserialize',
    SERDES_SERIALIZE = 'Serdes.serialize',
    METRICS_COUNTER_INC = 'Metrics.Counter.increment',
    DATAHUB_TRANSFORM = 'DataHub.transform',
  }

  export enum Handle {
    INPUT = 'input',
    OUTPUT = 'output',
    SCHEMA = 'schema',
    FUNCTION = 'function',
    SERIALISER = 'serialiser',
    DESERIALISER = 'deserialiser',
  }

  export interface DataHubTransformType {
    transform: string[]
  }
}

// TODO[18757] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18757/details/
export enum BehaviorPolicyType {
  MQTT_EVENT = 'Mqtt.events',
  PUBLISH_DUPLICATE = 'Publish.duplicate',
  PUBLISH_QUOTA = 'Publish.quota',
}

export interface BehaviorPolicyData extends DataHubNodeData {
  model: BehaviorPolicyType
  arguments?: Record<string, string | number>
  core?: BehaviorPolicy
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace BehaviorPolicyData {
  export enum Handle {
    CLIENT_FILTER = 'clientFilter',
  }
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

export interface TransitionData extends DataHubNodeData {
  model?: BehaviorPolicyType
  event?: TransitionType
  from?: StateType
  to?: StateType
  core?: BehaviorPolicyOnTransition
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace TransitionData {
  export enum Handle {
    BEHAVIOR_POLICY = 'target',
    OPERATION = 'source',
  }
}

export interface FsmState {
  name: string
  description: string
  type: string
}
export interface FsmTransition {
  fromState: string
  toState: string
  description: string
  event: string
}

export interface FiniteStateMachine {
  states: Array<FsmState>
  transitions: Array<FsmTransition>
}
export interface FiniteStateMachineSchema {
  metadata: FiniteStateMachine
}

export interface DryRunResults<T, R = never> {
  node: Node<DataHubNodeData>
  data?: T
  error?: ProblemDetailsExtended
  resources?: DryRunResults<R>[]
}

export enum PolicyDryRunStatus {
  IDLE = 'IDLE',
  RUNNING = 'RUNNING',
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
}
