import type { FC, FormEvent } from 'react'
import validator from '@rjsf/validator-ajv8'
import type { RJSFSchema, UiSchema, RJSFValidationError, RegistryFieldsType } from '@rjsf/utils'
import Form from '@rjsf/chakra-ui'
import type { IChangeEvent } from '@rjsf/core'

import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'

interface RjsfMocksProps {
  schema: RJSFSchema
  uiSchema?: UiSchema | undefined
  onSubmit?: (data: IChangeEvent, event: FormEvent) => void
  onError?: (errors: RJSFValidationError[]) => void
  formData?: unknown
  fields?: RegistryFieldsType
}

const RjsfMocks: FC<RjsfMocksProps> = ({ schema, uiSchema, formData, fields, onSubmit, onError }) => {
  return (
    <Form
      id="mock-jsonschema-form"
      schema={schema}
      uiSchema={uiSchema}
      templates={{
        ObjectFieldTemplate,
        FieldTemplate,
        BaseInputTemplate,
        ArrayFieldTemplate,
        ArrayFieldItemTemplate,
      }}
      liveValidate
      showErrorList="bottom"
      validator={validator}
      onSubmit={onSubmit}
      onError={onError}
      formData={formData}
      fields={fields}
    />
  )
}

export default RjsfMocks
