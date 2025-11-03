import type { Node } from '@xyflow/react'
import type { BehaviorPolicy, DataPolicy, PolicySchema, Script } from '@/api/__generated__'
import {
  DataHubNodeType,
  ResourceWorkingVersion,
  BehaviorPolicyType,
  SchemaType,
  type DryRunResults,
  type SchemaData,
  type FunctionData,
  type DataPolicyData,
  type BehaviorPolicyData,
} from '@datahub/types.ts'

// Mock Nodes
export const MOCK_POLICY_NODE_DATA: Node<DataPolicyData> = {
  id: 'policy-node-1',
  type: DataHubNodeType.DATA_POLICY,
  position: { x: 0, y: 0 },
  data: {
    id: 'my-data-policy',
  },
}

export const MOCK_BEHAVIOR_POLICY_NODE: Node<BehaviorPolicyData> = {
  id: 'behavior-policy-node-1',
  type: DataHubNodeType.BEHAVIOR_POLICY,
  position: { x: 0, y: 0 },
  data: {
    id: 'my-behavior-policy',
    model: BehaviorPolicyType.MQTT_EVENT,
  },
}

export const MOCK_SCHEMA_NODE_DRAFT: Node<SchemaData> = {
  id: 'schema-node-1',
  type: DataHubNodeType.SCHEMA,
  position: { x: 0, y: 0 },
  data: {
    name: 'temperature-schema',
    version: ResourceWorkingVersion.DRAFT,
    type: SchemaType.JSON,
  },
}

export const MOCK_SCHEMA_NODE_MODIFIED: Node<SchemaData> = {
  id: 'schema-node-2',
  type: DataHubNodeType.SCHEMA,
  position: { x: 0, y: 0 },
  data: {
    name: 'humidity-schema',
    version: ResourceWorkingVersion.MODIFIED,
    type: SchemaType.JSON,
  },
}

export const MOCK_SCRIPT_NODE_DRAFT: Node<FunctionData> = {
  id: 'script-node-1',
  type: DataHubNodeType.FUNCTION,
  position: { x: 0, y: 0 },
  data: {
    name: 'transform-temperature',
    version: ResourceWorkingVersion.DRAFT,
    type: 'Javascript',
  },
}

// Mock Policy Data
export const MOCK_DATA_POLICY: DataPolicy = {
  id: 'my-data-policy',
  matching: {
    topicFilter: 'devices/+/temperature', // API uses singular
  },
  validation: {
    validators: [],
  },
  onSuccess: {
    pipeline: [],
  },
}

export const MOCK_BEHAVIOR_POLICY: BehaviorPolicy = {
  id: 'my-behavior-policy',
  matching: {
    clientIdRegex: '.*',
  },
  behavior: {
    id: 'publish-quota-policy',
    arguments: {
      minPublishes: 1,
      maxPublishes: 100,
    },
  },
  onTransitions: {
    'Mqtt.OnInboundPublish': {
      pipeline: [],
    },
  },
}

// Mock Resource Data
export const MOCK_SCHEMA: PolicySchema = {
  id: 'temperature-schema',
  version: 1,
  type: 'JSON',
  schemaDefinition: '{"type": "object"}',
  createdAt: '2025-11-03T10:00:00Z',
}

export const MOCK_SCHEMA_MODIFIED: PolicySchema = {
  id: 'humidity-schema',
  version: 2,
  type: 'JSON',
  schemaDefinition: '{"type": "object"}',
  createdAt: '2025-11-03T09:00:00Z',
}

export const MOCK_SCRIPT: Script = {
  id: 'transform-temperature',
  version: 1,
  functionType: 'TRANSFORMATION' as Script.functionType,
  source: 'function transform(value) { return value * 1.8 + 32; }',
  createdAt: '2025-11-03T10:00:00Z',
}

// Mock Validation Reports

/**
 * Success report for a Data Policy with schemas and scripts
 */
export const MOCK_SUCCESS_REPORT_DATA_POLICY: DryRunResults<unknown, never>[] = [
  // Per-node validation items
  {
    node: {
      id: 'topic-filter-1',
      type: DataHubNodeType.TOPIC_FILTER,
      position: { x: 0, y: 0 },
      data: {},
    },
    data: { topics: ['devices/+/temperature'] },
    error: undefined,
  },
  {
    node: MOCK_SCHEMA_NODE_DRAFT,
    data: MOCK_SCHEMA,
    error: undefined,
  },
  {
    node: MOCK_SCRIPT_NODE_DRAFT,
    data: MOCK_SCRIPT,
    error: undefined,
  },
  {
    node: MOCK_POLICY_NODE_DATA,
    data: { id: 'my-data-policy' },
    error: undefined,
  },
  // Final summary item (most important!)
  {
    node: MOCK_POLICY_NODE_DATA,
    data: MOCK_DATA_POLICY,
    error: undefined,
    resources: [
      {
        node: MOCK_SCHEMA_NODE_DRAFT,
        data: MOCK_SCHEMA as unknown,
        error: undefined,
      },
      {
        node: MOCK_SCRIPT_NODE_DRAFT,
        data: MOCK_SCRIPT as unknown,
        error: undefined,
      },
    ],
  },
]

/**
 * Success report for a Behavior Policy with transitions
 */
export const MOCK_SUCCESS_REPORT_BEHAVIOR_POLICY: DryRunResults<unknown, never>[] = [
  // Per-node validation items
  {
    node: {
      id: 'client-filter-1',
      type: DataHubNodeType.CLIENT_FILTER,
      position: { x: 0, y: 0 },
      data: {},
    },
    data: { clients: ['.*'] },
    error: undefined,
  },
  {
    node: MOCK_BEHAVIOR_POLICY_NODE,
    data: { id: 'my-behavior-policy' },
    error: undefined,
  },
  // Final summary item
  {
    node: MOCK_BEHAVIOR_POLICY_NODE,
    data: MOCK_BEHAVIOR_POLICY,
    error: undefined,
    resources: [],
  },
]

/**
 * Success report with no additional resources
 */
export const MOCK_SUCCESS_REPORT_NO_RESOURCES: DryRunResults<unknown, never>[] = [
  {
    node: MOCK_POLICY_NODE_DATA,
    data: { id: 'simple-policy' },
    error: undefined,
  },
  // Final summary item with no resources
  {
    node: MOCK_POLICY_NODE_DATA,
    data: {
      id: 'simple-policy',
      matching: {
        topicFilter: 'test/topic', // API uses singular
      },
      validation: {
        validators: [],
      },
      onSuccess: {
        pipeline: [],
      },
    },
    error: undefined,
    resources: [],
  },
]

/**
 * Success report with multiple schemas (new and modified)
 */
export const MOCK_SUCCESS_REPORT_MIXED_RESOURCES: DryRunResults<unknown, never>[] = [
  // Per-node items
  {
    node: MOCK_SCHEMA_NODE_DRAFT,
    data: MOCK_SCHEMA,
    error: undefined,
  },
  {
    node: MOCK_SCHEMA_NODE_MODIFIED,
    data: MOCK_SCHEMA_MODIFIED,
    error: undefined,
  },
  {
    node: MOCK_POLICY_NODE_DATA,
    data: { id: 'mixed-policy' },
    error: undefined,
  },
  // Final summary item
  {
    node: MOCK_POLICY_NODE_DATA,
    data: MOCK_DATA_POLICY,
    error: undefined,
    resources: [
      {
        node: MOCK_SCHEMA_NODE_DRAFT, // DRAFT = new
        data: MOCK_SCHEMA as unknown,
        error: undefined,
      },
      {
        node: MOCK_SCHEMA_NODE_MODIFIED, // MODIFIED = update
        data: MOCK_SCHEMA_MODIFIED as unknown,
        error: undefined,
      },
    ],
  },
]

/**
 * Empty report (edge case)
 */
export const MOCK_EMPTY_REPORT: DryRunResults<unknown, never>[] = []

/**
 * Report with no final summary item (malformed)
 */
export const MOCK_MALFORMED_REPORT: DryRunResults<unknown, never>[] = [
  {
    node: MOCK_POLICY_NODE_DATA,
    data: undefined, // Missing data
    error: undefined,
  },
]
