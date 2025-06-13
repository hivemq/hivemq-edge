import type { FC } from 'react'
import validator from '@rjsf/validator-ajv8'

import Form from '@rjsf/chakra-ui'
import type { FormProps } from '@rjsf/core'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate'
import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate'
import { DescriptionFieldTemplate, ErrorListTemplate, TitleFieldTemplate } from '@/components/rjsf/Templates'

interface CustomFormTestingProps
  extends Pick<
    FormProps<unknown>,
    'schema' | 'uiSchema' | 'formData' | 'onChange' | 'onSubmit' | 'onError' | 'formContext' | 'widgets'
  > {
  id?: string
}

export const CustomFormTesting: FC<CustomFormTestingProps> = ({
  schema,
  uiSchema,
  formData,
  formContext,
  widgets,
  onChange,
  onError,
  onSubmit,
}) => {
  return (
    <Form
      schema={schema}
      validator={validator}
      uiSchema={uiSchema}
      formData={formData}
      formContext={formContext}
      onChange={onChange}
      onError={onError}
      onSubmit={onSubmit}
      liveValidate
      showErrorList="bottom"
      widgets={widgets}
      templates={{
        ObjectFieldTemplate,
        FieldTemplate,
        BaseInputTemplate,
        ArrayFieldTemplate,
        ArrayFieldItemTemplate,
        DescriptionFieldTemplate,
        ErrorListTemplate,
        TitleFieldTemplate,
      }}
    />
  )
}
