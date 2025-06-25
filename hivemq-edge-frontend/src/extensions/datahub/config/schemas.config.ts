import type { RJSFSchema } from '@rjsf/utils'
import { DataHubNodeType } from '@datahub/types.ts'

import type { FunctionSpecs } from '@/api/__generated__'

import { MOCK_TOPIC_FILTER_SCHEMA } from '@datahub/designer/topic_filter/TopicFilterData.ts'
import { MOCK_CLIENT_FILTER_SCHEMA } from '@datahub/designer/client_filter/ClientFilterSchema.ts'
import { MOCK_DATA_POLICY_SCHEMA } from '@datahub/designer/data_policy/DataPolicySchema.ts'
import { MOCK_VALIDATOR_SCHEMA } from '@datahub/designer/validator/DataPolicyValidator.ts'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'
import { MOCK_FUNCTION_SCHEMA } from '@datahub/designer/script/FunctionData.ts'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'
import { MOCK_TRANSITION_SCHEMA } from '@datahub/designer/transition/TransitionData.ts'
import { getOperationSchema } from '@datahub/designer/operation/OperationPanel.utils.ts'

export const getNodeValidationSchema = (type: string, functions?: FunctionSpecs[]): RJSFSchema | undefined => {
  switch (type) {
    case DataHubNodeType.VALIDATOR:
      return MOCK_VALIDATOR_SCHEMA.schema
    case DataHubNodeType.TRANSITION:
      return MOCK_TRANSITION_SCHEMA.schema
    case DataHubNodeType.TOPIC_FILTER:
      return MOCK_TOPIC_FILTER_SCHEMA.schema
    case DataHubNodeType.FUNCTION:
      return MOCK_FUNCTION_SCHEMA.schema
    case DataHubNodeType.SCHEMA:
      return MOCK_SCHEMA_SCHEMA.schema
    case DataHubNodeType.DATA_POLICY:
      return MOCK_DATA_POLICY_SCHEMA.schema
    case DataHubNodeType.CLIENT_FILTER:
      return MOCK_CLIENT_FILTER_SCHEMA.schema
    case DataHubNodeType.BEHAVIOR_POLICY:
      return MOCK_BEHAVIOR_POLICY_SCHEMA.schema
    case DataHubNodeType.OPERATION:
      return getOperationSchema(functions || []).schema
    default:
      return undefined
  }
}
