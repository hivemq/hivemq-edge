import { FC } from 'react'
import { NodeProps } from 'reactflow'

import { DataHubNodeType, PanelProps } from '@datahub/types.ts'
import { TopicFilterPanel } from '@datahub/designer/topic_filter/TopicFilterPanel.tsx'
import { ClientFilterPanel } from '@datahub/designer/client_filter/ClientFilterPanel.tsx'
import { ValidatorPanel } from '@datahub/designer/validator/ValidatorPanel.tsx'
import { SchemaPanel } from '@datahub/designer/schema/SchemaPanel.tsx'
import { BehaviorPolicyPanel } from '@datahub/designer/behavior_policy/BehaviorPolicyPanel.tsx'
import { TransitionPanel } from '@datahub/designer/transition/TransitionPanel.tsx'
import { OperationPanel } from '@datahub/designer/operation/OperationPanel.tsx'
import { FunctionPanel } from '@datahub/designer/script/FunctionPanel.tsx'
import { TopicFilterNode } from '@datahub/designer/topic_filter/TopicFilterNode.tsx'
import { ClientFilterNode } from '@datahub/designer/client_filter/ClientFilterNode.tsx'
import { DataPolicyNode } from '@datahub/designer/data_policy/DataPolicyNode.tsx'
import { ValidatorNode } from '@datahub/designer/validator/ValidatorNode.tsx'
import { SchemaNode } from '@datahub/designer/schema/SchemaNode.tsx'
import { OperationNode } from '@datahub/designer/operation/OperationNode.tsx'
import { FunctionNode } from '@datahub/designer/script/FunctionNode.tsx'
import { BehaviorPolicyNode } from '@datahub/designer/behavior_policy/BehaviorPolicyNode.tsx'
import { TransitionNode } from '@datahub/designer/transition/TransitionNode.tsx'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { DataPolicyPanel } from '@datahub/designer/data_policy/DataPolicyPanel.tsx'

/**
 * Used in the side panel editor to render the content of the selected node
 */
export const DefaultEditor: Record<string, FC<PanelProps>> = {
  [DataHubNodeType.INTERNAL]: () => <LoaderSpinner />,
  [DataHubNodeType.TOPIC_FILTER]: TopicFilterPanel,
  [DataHubNodeType.CLIENT_FILTER]: ClientFilterPanel,
  [DataHubNodeType.DATA_POLICY]: DataPolicyPanel,
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
