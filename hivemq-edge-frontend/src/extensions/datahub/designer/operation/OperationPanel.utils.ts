import type { FunctionSpecs } from '@/api/__generated__'
import { MOCK_OPERATION_SCHEMA } from '@datahub/designer/operation/OperationData.ts'
import type { PanelSpecs } from '@datahub/types.ts'

export const getOperationSchema = (functions: FunctionSpecs[]): PanelSpecs => {
  const template = MOCK_OPERATION_SCHEMA.schema

  functions.forEach((f) => {
    template.definitions = { ...template.definitions, [f.functionId]: f.schema }
  })

  //
  // @ts-ignore This is a total hack
  template.definitions.functionId.properties.functionId.enum = functions.map((e) => e.functionId)
  // @ts-ignore This is a total hack
  template.definitions.functionId.dependencies.functionId.oneOf = functions.map((e) => ({
    properties: {
      functionId: {
        enum: [e.functionId],
      },
      formData: {
        $ref: `#/definitions/${e.functionId}`,
      },
    },
  }))

  return { schema: template, uiSchema: MOCK_OPERATION_SCHEMA.uiSchema }
}
