import type { FC } from 'react'
import type { EdgeProps, NodeProps } from 'reactflow'
import { DataHubNodeType, EdgeTypes } from '@datahub/types.ts'
import { TopicFilterNode } from '@datahub/designer/topic_filter/TopicFilterNode.tsx'
import { ClientFilterNode } from '@datahub/designer/client_filter/ClientFilterNode.tsx'
import { DataPolicyNode } from '@datahub/designer/data_policy/DataPolicyNode.tsx'
import { ValidatorNode } from '@datahub/designer/validator/ValidatorNode.tsx'
import { SchemaNode } from '@datahub/designer/schema/SchemaNode.tsx'
import { OperationNode } from '@datahub/designer/operation/OperationNode.tsx'
import { FunctionNode } from '@datahub/designer/script/FunctionNode.tsx'
import { BehaviorPolicyNode } from '@datahub/designer/behavior_policy/BehaviorPolicyNode.tsx'
import { TransitionNode } from '@datahub/designer/transition/TransitionNode.tsx'
import DataHubPolicyEdge from '@datahub/components/edges/DataHubPolicyEdge.tsx'

/**
 * Used in the ReactFlow component to create custom nodes
 */
export const CustomNodeTypes: Record<string, FC<NodeProps>> = {
  [DataHubNodeType.TOPIC_FILTER]: TopicFilterNode,
  [DataHubNodeType.CLIENT_FILTER]: ClientFilterNode,
  [DataHubNodeType.DATA_POLICY]: DataPolicyNode,
  [DataHubNodeType.VALIDATOR]: ValidatorNode,
  [DataHubNodeType.SCHEMA]: SchemaNode,
  [DataHubNodeType.OPERATION]: OperationNode,
  [DataHubNodeType.FUNCTION]: FunctionNode,
  [DataHubNodeType.BEHAVIOR_POLICY]: BehaviorPolicyNode,
  [DataHubNodeType.TRANSITION]: TransitionNode,
}

/**
 * Used in the ReactFlow component to create custom edges
 */
export const CustomEdgeTypes: Record<string, FC<EdgeProps>> = {
  [EdgeTypes.DATAHUB_EDGE]: DataHubPolicyEdge,
}
