import { FC } from 'react'
import { NodeProps } from 'reactflow'

import { DataHubNodeType, PanelProps } from '@datahub/types.ts'
import { TopicFilterPanel } from '@datahub/flow/topic_filter/TopicFilterPanel.tsx'
import { ClientFilterPanel } from '@datahub/flow/client_filter/ClientFilterPanel.tsx'
import { ValidatorPanel } from '@datahub/flow/validator/ValidatorPanel.tsx'
import { SchemaPanel } from '@datahub/flow/schema/SchemaPanel.tsx'
import { BehaviorPolicyPanel } from '@datahub/flow/behavior_policy/BehaviorPolicyPanel.tsx'
import { TransitionPanel } from '@datahub/flow/transition/TransitionPanel.tsx'
import { OperationPanel } from '@datahub/flow/operation/OperationPanel.tsx'
import { FunctionPanel } from '@datahub/flow/script/FunctionPanel.tsx'
import { TopicFilterNode } from '@datahub/flow/topic_filter/TopicFilterNode.tsx'
import { ClientFilterNode } from '@datahub/flow/client_filter/ClientFilterNode.tsx'
import { DataPolicyNode } from '@datahub/flow/data_policy/DataPolicyNode.tsx'
import { ValidatorNode } from '@datahub/flow/validator/ValidatorNode.tsx'
import { SchemaNode } from '@datahub/flow/schema/SchemaNode.tsx'
import { OperationNode } from '@datahub/flow/operation/OperationNode.tsx'
import { FunctionNode } from '@datahub/flow/script/FunctionNode.tsx'
import { BehaviorPolicyNode } from '@datahub/flow/behavior_policy/BehaviorPolicyNode.tsx'
import { TransitionNode } from '@datahub/flow/transition/TransitionNode.tsx'

/**
 * Used in the side panel editor to render the content of the selected node
 */
export const DefaultEditor: Record<string, FC<PanelProps>> = {
  [DataHubNodeType.TOPIC_FILTER]: TopicFilterPanel,
  [DataHubNodeType.CLIENT_FILTER]: ClientFilterPanel,
  [DataHubNodeType.VALIDATOR]: ValidatorPanel,
  [DataHubNodeType.SCHEMA]: SchemaPanel,
  [DataHubNodeType.BEHAVIOR_POLICY]: BehaviorPolicyPanel,
  [DataHubNodeType.TRANSITION]: TransitionPanel,
  [DataHubNodeType.OPERATION]: OperationPanel,
  [DataHubNodeType.FUNCTION]: FunctionPanel,
}

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
