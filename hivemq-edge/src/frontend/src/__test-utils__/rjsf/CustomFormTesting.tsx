import type { FC } from 'react'
import validator from '@rjsf/validator-ajv8'

import Form from '@rjsf/chakra-ui'
import type { FormProps } from '@rjsf/core'

interface CustomFormTestingProps
  extends Pick<FormProps<unknown>, 'schema' | 'uiSchema' | 'formData' | 'onChange' | 'onSubmit' | 'onError'> {
  id?: string
}

export const CustomFormTesting: FC<CustomFormTestingProps> = ({
  schema,
  uiSchema,
  formData,
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
      onChange={onChange}
      onError={onError}
      onSubmit={onSubmit}
      liveValidate
      showErrorList="bottom"
    />
  )
}
