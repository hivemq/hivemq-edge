import { FC } from 'react'
import { NodeProps } from 'reactflow'
import { RJSFSchema } from '@rjsf/utils'

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
import { MOCK_TOPIC_FILTER_SCHEMA } from '@datahub/designer/topic_filter/TopicFilterData.ts'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'
import { MOCK_CLIENT_FILTER_SCHEMA } from '@datahub/designer/client_filter/ClientFilterSchema.ts'
import { MOCK_DATA_POLICY_SCHEMA } from '@datahub/designer/data_policy/DataPolicySchema.ts'
import { MOCK_OPERATION_SCHEMA } from '@datahub/designer/operation/OperationData.ts'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'
import { MOCK_FUNCTION_SCHEMA } from '@datahub/designer/script/FunctionData.ts'
import { MOCK_TRANSITION_SCHEMA } from '@datahub/designer/transition/TransitionData.ts'
import { MOCK_VALIDATOR_SCHEMA } from '@datahub/designer/validator/DataPolicyValidator.ts'

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

export const CustomNodeJSONSchema: Record<string, RJSFSchema> = {
  [DataHubNodeType.TOPIC_FILTER]: MOCK_TOPIC_FILTER_SCHEMA.schema,
  [DataHubNodeType.CLIENT_FILTER]: MOCK_CLIENT_FILTER_SCHEMA.schema,
  [DataHubNodeType.DATA_POLICY]: MOCK_DATA_POLICY_SCHEMA.schema,
  [DataHubNodeType.VALIDATOR]: MOCK_VALIDATOR_SCHEMA,
  [DataHubNodeType.SCHEMA]: MOCK_SCHEMA_SCHEMA.schema,
  [DataHubNodeType.OPERATION]: MOCK_OPERATION_SCHEMA.schema,
  [DataHubNodeType.FUNCTION]: MOCK_FUNCTION_SCHEMA.schema,
  [DataHubNodeType.BEHAVIOR_POLICY]: MOCK_BEHAVIOR_POLICY_SCHEMA.schema,
  [DataHubNodeType.TRANSITION]: MOCK_TRANSITION_SCHEMA.schema,
}
