import type { FC } from 'react'
import type { PanelProps } from '@datahub/types.ts'
import { DataHubNodeType } from '@datahub/types.ts'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { TopicFilterPanel } from '@datahub/designer/topic_filter/TopicFilterPanel.tsx'
import { ClientFilterPanel } from '@datahub/designer/client_filter/ClientFilterPanel.tsx'
import { DataPolicyPanel } from '@datahub/designer/data_policy/DataPolicyPanel.tsx'
import { ValidatorPanel } from '@datahub/designer/validator/ValidatorPanel.tsx'
import { SchemaPanel } from '@datahub/designer/schema/SchemaPanel.tsx'
import { BehaviorPolicyPanel } from '@datahub/designer/behavior_policy/BehaviorPolicyPanel.tsx'
import { TransitionPanel } from '@datahub/designer/transition/TransitionPanel.tsx'
import { OperationPanel } from '@datahub/designer/operation/OperationPanel.tsx'
import { FunctionPanel } from '@datahub/designer/script/FunctionPanel.tsx'

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
