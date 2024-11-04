import { FC, useCallback, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import debug from 'debug'
import { immutableJSONPatch, JSONPatchAdd, JSONPatchDocument } from 'immutable-json-patch'
import Form from '@rjsf/chakra-ui'
import { FormProps, IChangeEvent } from '@rjsf/core'
import { IdSchema } from '@rjsf/utils'
import { RJSFValidationError } from '@rjsf/utils/src/types.ts'
import validator from '@rjsf/validator-ajv8'

import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { DescriptionFieldTemplate } from '@/components/rjsf/Templates/DescriptionFieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import { ErrorListTemplate } from '@/components/rjsf/Templates/ErrorListTemplate.tsx'
import { ChakraRJSFormContext } from '@/components/rjsf/Form/types.ts'
import { customFormatsValidator } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import { adapterJSFFields, adapterJSFWidgets } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import { TitleFieldTemplate } from '@/components/rjsf/Templates/TitleFieldTemplate.tsx'

interface CustomFormProps<T>
  extends Pick<
    FormProps<T>,
    'id' | 'schema' | 'uiSchema' | 'formData' | 'formContext' | 'customValidate' | 'readonly'
  > {
  onSubmit: (data: T) => void
}

const FLAG_POST_VALIDATE = false

const ChakraRJSForm: FC<CustomFormProps<any>> = ({
  id,
  schema,
  uiSchema,
  formData,
  onSubmit,
  formContext,
  customValidate,
  readonly,
}) => {
  const { t } = useTranslation()
  const [batchData, setBatchData] = useState<JSONPatchDocument | undefined>(undefined)
  const defaultValues = useMemo(() => {
    if (batchData) {
      return immutableJSONPatch(formData, batchData)
    }
    return formData
  }, [batchData, formData])

  const filterUnboundErrors = (errors: RJSFValidationError[]) => {
    // Hide the AJV8 validation error from the view. It has no other identifier so matching the text
    return errors.filter((error) => !error.stack.startsWith('no schema with key or ref'))
  }

  const onValidate = useCallback(
    (data: IChangeEvent<any>) => {
      onSubmit?.(data)
    },
    [onSubmit]
  )

  const context: ChakraRJSFormContext = {
    ...formContext,
    onBatchUpload: (idSchema: IdSchema<unknown>, batch) => {
      const path = idSchema.$id.replace('root_', '/').replaceAll('_', '/') + '/-'
      const operations: JSONPatchDocument = batch.map<JSONPatchAdd>((value) => ({ op: 'add', path, value }))

      if (schema && FLAG_POST_VALIDATE) {
        const updatedDocument = immutableJSONPatch(defaultValues, operations)
        const { $schema, ...rest } = schema
        const validate = validator.ajv.compile(rest)
        validate(updatedDocument)
      }

      setBatchData(operations)
    },
  }

  const rjsfLog = debug(`RJSF:${id}`)

  return (
    <Form
      id={id}
      readonly={readonly}
      schema={schema}
      uiSchema={uiSchema}
      formData={defaultValues}
      formContext={context}
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
      liveValidate
      // TODO[NVL] Strange lack of initial validation; preventing it by enforcing HTML validation
      // noHtml5Validate
      focusOnFirstError
      onSubmit={onValidate}
      validator={customFormatsValidator}
      customValidate={customValidate}
      transformErrors={filterUnboundErrors}
      widgets={adapterJSFWidgets}
      fields={adapterJSFFields}
      onError={(errors) => rjsfLog(t('error.rjsf.validation'), errors)}
      showErrorList="bottom"
    />
  )
}

export default ChakraRJSForm
