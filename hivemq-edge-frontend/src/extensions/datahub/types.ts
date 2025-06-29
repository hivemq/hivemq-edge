import type { Connection, Edge, EdgeAddChange, Node, NodeAddChange, OnEdgesChange, OnNodesChange } from '@xyflow/react'
import type {
  BehaviorPolicy,
  BehaviorPolicyOnTransition,
  BehaviorPolicyTransitionEvent,
  DataPolicy,
  DataPolicyMatching,
  DataPolicyValidator,
  PolicyOperation,
  PolicySchema,
  SchemaReference,
  Script,
} from '@/api/__generated__'
import { PolicyType } from '@/api/__generated__'
import type { RJSFSchema, UiSchema } from '@rjsf/utils'
import type { IChangeEvent } from '@rjsf/core'
import type { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'

export interface HotKeyItem {
  key: string
  category: string
}

export interface PanelSpecs {
  schema: RJSFSchema
  uiSchema?: UiSchema
}

export interface PanelProps {
  selectedNode: string
  onFormSubmit?: (data: IChangeEvent) => void
  onFormError?: (error: Error) => void
}

export enum DesignerStatus {
  DRAFT = 'DRAFT',
  LOADED = 'LOADED',
  MODIFIED = 'MODIFIED',
}

export interface WorkspaceState {
  nodes: Node[]
  edges: Edge[]
}

export interface WorkspaceStatus {
  status: DesignerStatus
  name: string
  type: DataHubNodeType.DATA_POLICY | DataHubNodeType.BEHAVIOR_POLICY | undefined
}

export interface WorkspaceAction {
  reset: () => void
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onConnect: (connection: Connection) => void
  onAddNodes: (changes: NodeAddChange[]) => void
  onAddEdges: (changes: EdgeAddChange[]) => void
  onUpdateNodes: <T>(item: string, data: T) => void

  onSerializePolicy: (node: Node<DataPolicyData | BehaviorPolicyData>) => string | undefined

  isDirty: () => boolean
  isPolicyInDraft: () => boolean

  setStatus: (
    status: DesignerStatus,
    option?: { name?: string; type?: DataHubNodeType.DATA_POLICY | DataHubNodeType.BEHAVIOR_POLICY }
  ) => void
}

export interface PolicyCheckState {
  // TODO[30991] The bug is fixed but replace with a reference to the id, NOT the node itself!
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

export enum EdgeTypes {
  DATAHUB_EDGE = 'DATAHUB_EDGE',
}

export enum DataHubNodeType {
  INTERNAL = 'INTERNAL',
  ADAPTOR = 'ADAPTOR',
  EDGE = 'EDGE',
  BRIDGE = 'BRIDGE',
  TOPIC_FILTER = 'TOPIC_FILTER',
  CLIENT_FILTER = 'CLIENT_FILTER',
  DATA_POLICY = PolicyType.DATA_POLICY,
  BEHAVIOR_POLICY = PolicyType.BEHAVIOR_POLICY,
  VALIDATOR = 'VALIDATOR',
  SCHEMA = 'SCHEMA',
  OPERATION = 'OPERATION',
  TRANSITION = 'TRANSITION',
  FUNCTION = 'FUNCTION',
  EVENT = 'EVENT',
}

export enum DesignerPolicyType {
  CREATE_POLICY = 'CREATE_POLICY',
  DATA_POLICY = PolicyType.DATA_POLICY,
  BEHAVIOR_POLICY = PolicyType.BEHAVIOR_POLICY,
}

export enum NodeCategory {
  DEFAULT = 'DEFAULT',
  INITIAL = 'INITIAL',
  POLICY = 'POLICY',
  RESOURCE = 'RESOURCE',
}

export type DataHubNodeData = {
  dryRunStatus?: PolicyDryRunStatus
}

export type TopicFilterData = DataHubNodeData & {
  adapter?: string
  topics: string[]
}

export type ClientFilterData = DataHubNodeData & {
  clients: string[]
}

export interface DataPolicyData extends DataHubNodeData {
  id: string
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
export enum StrategyType {
  ALL_OF = 'ALL_OF',
  ANY_OF = 'ANY_OF',
}

export interface SchemaArguments {
  schemas: SchemaReference[]
  strategy: StrategyType
}

export type ValidatorData = DataHubNodeData & {
  type: DataPolicyValidator.type
  strategy: StrategyType
  schemas: SchemaReference[]
  core?: DataPolicyValidator
}

export enum ResourceStatus {
  DRAFT = 'DRAFT',
  LOADED = 'LOADED',
  MODIFIED = 'MODIFIED',
}

// TODO[24146] Should be safe for incremental version number but better identification?
export enum ResourceWorkingVersion {
  DRAFT = 9 ** 9,
  LOADED,
  MODIFIED,
}

export type ResourceState = DataHubNodeData & {
  version: number
  internalStatus?: ResourceStatus
  internalVersions?: number[]
}

export interface ResourceFamily {
  name: string
  versions: number[]
  description?: string
  type?: string
  label?: string
  internalStatus?: ResourceStatus
}

// TODO[18755] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18755/details/
export enum SchemaType {
  JSON = 'JSON',
  PROTOBUF = 'PROTOBUF',
}

export interface SchemaProtobufArguments {
  messageType: string
}

export type SchemaData = ResourceState & {
  name: string
  type: SchemaType
  schemaSource?: string
  messageType?: string
  core?: PolicySchema
}

export interface FunctionData extends ResourceState {
  name: string
  type: 'Javascript'
  description?: string
  sourceCode?: string
  core?: Script
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
  id: string
  functionId?: string
  metadata?: FunctionDefinition
  formData?: Record<string, string | number | string[] | boolean>
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

// TODO[33539] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18757/details/
export enum BehaviorPolicyType {
  MQTT_EVENT = 'Mqtt.events',
  PUBLISH_DUPLICATE = 'Publish.duplicate',
  PUBLISH_QUOTA = 'Publish.quota',
}

// TODO[33539] Add to the OpenAPI specs; see https://hivemq.kanbanize.com/ctrl_board/4/cards/18757/details/
export interface PublishQuotaArguments {
  minPublishes: number
  maxPublishes: number
}

export interface BehaviorPolicyData extends DataHubNodeData {
  id: string
  model: BehaviorPolicyType
  arguments?: PublishQuotaArguments | Record<string, string | number>
  core?: BehaviorPolicy
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace BehaviorPolicyData {
  export enum Handle {
    CLIENT_FILTER = 'clientFilter',
    TRANSITIONS = 'transitions',
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

export interface TransitionData extends DataHubNodeData {
  model?: BehaviorPolicyType
  event?: BehaviorPolicyTransitionEvent
  from?: StateType
  to?: StateType
  type?: FsmState.Type
  core?: BehaviorPolicyOnTransition
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace TransitionData {
  export enum Handle {
    BEHAVIOR_POLICY = 'target',
    OPERATION = 'source',
    ON_SUCCESS = 'onSuccess',
    ON_ERROR = 'onError',
  }
}

export type FsmState = {
  name: string
  description: string
  type: FsmState.Type
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FsmState {
  export enum Type {
    INITIAL = 'INITIAL',
    INTERMEDIATE = 'INTERMEDIATE',
    SUCCESS = 'SUCCESS',
    FAILED = 'FAILED',
  }
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
  node: Node
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

export interface ValidDropConnection {
  type: DataHubNodeType
  handle: string | null
  isSource: boolean
}

export interface DraftPolicy {
  readonly createdAt?: string
  id: string
  matching: DataPolicyMatching
}

export type CombinedPolicy =
  | (DataPolicy & { type: DesignerPolicyType.DATA_POLICY })
  | (BehaviorPolicy & { type: DesignerPolicyType.BEHAVIOR_POLICY })
  | (DraftPolicy & { type: DesignerPolicyType.CREATE_POLICY })

export interface ReactFlowSchemaFormContext {
  functions: FunctionSpecs[]
}
