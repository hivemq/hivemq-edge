import { PanelSpecs } from '@/extensions/datahub/types.ts'
import { datahubInternalFunctions } from '@/extensions/datahub/api/specs/function.ts'
import { RJSFSchema } from '@rjsf/utils'
import { StrictRJSFSchema } from '@rjsf/utils/src/types.ts'

import schema from '../__generated__/schemas/OperationData.json'

const enums = datahubInternalFunctions.map((e) => {
  const { schema, uiSchema, hasArguments, ...rest } = e
  return rest
})

const dependencies = datahubInternalFunctions.map<RJSFSchema>((e) => {
  return {
    properties: {
      action: {
        enum: [e.functionId],
      },
      formData: { $ref: `#/definitions/${e.functionId}` },
    },
  }
})

const definitions = datahubInternalFunctions.reduce((a, v) => {
  if (!v.schema) return {}

  const { title, ...rest } = v.schema
  return {
    ...a,
    [v.functionId]: {
      title,
      metadata: {
        isTerminal: v.isTerminal,
        hasArguments: v.hasArguments,
        isDataOnly: v.isDataOnly,
      },
      ...rest,
    },
  }
}, {})

// @ts-ignore
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const generator: PanelSpecs = {
  schema: {
    $ref: '#/definitions/action',
    definitions: {
      ...definitions,
      action: {
        properties: {
          action: {
            title: 'Function',
            description: `You can use two categories of functions in your policies. Non-terminal functions allow further operations in the pipeline to be executed,
                          while terminal functions end further operations in the pipeline`,
            // @ts-ignore Part of RJSF, not JSON-SCHEMA. There are (better?) alternatives
            // enumNames: enums.map((e) => e.functionId),
            enum: enums.map((e) => e.functionId),
          },
        },
        dependencies: {
          action: {
            oneOf: [...dependencies],
          },
        },
      },
    },
  },
}

export const MOCK_OPERATION_SCHEMA: PanelSpecs = {
  schema: schema as StrictRJSFSchema,
  uiSchema: {
    action: {
      'ui:widget': 'datahub:function-selector',
    },
  },
}
