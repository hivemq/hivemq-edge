import type { RJSFSchema } from '@rjsf/utils'
import { DataHubNodeType } from '@datahub/types.ts'
import { MOCK_TOPIC_FILTER_SCHEMA } from '@datahub/designer/topic_filter/TopicFilterData.ts'
import { MOCK_CLIENT_FILTER_SCHEMA } from '@datahub/designer/client_filter/ClientFilterSchema.ts'
import { MOCK_DATA_POLICY_SCHEMA } from '@datahub/designer/data_policy/DataPolicySchema.ts'
import { MOCK_VALIDATOR_SCHEMA } from '@datahub/designer/validator/DataPolicyValidator.ts'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'
import { MOCK_OPERATION_SCHEMA } from '@datahub/designer/operation/OperationData.ts'
import { MOCK_FUNCTION_SCHEMA } from '@datahub/designer/script/FunctionData.ts'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'
import { MOCK_TRANSITION_SCHEMA } from '@datahub/designer/transition/TransitionData.ts'

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
