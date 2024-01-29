import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { RJSFSchema } from '@rjsf/utils'

import schema from '../__generated__/schemas/TransitionData.json'

export const MOCK_TRANSITION_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
}
