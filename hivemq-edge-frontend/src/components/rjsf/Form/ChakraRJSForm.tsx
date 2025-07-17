import type { FC } from 'react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import debug from 'debug'
import type { JSONPatchAdd, JSONPatchDocument } from 'immutable-json-patch'
import { immutableJSONPatch } from 'immutable-json-patch'
import Form from '@rjsf/chakra-ui'
import type { FormProps, IChangeEvent } from '@rjsf/core'
import type { IdSchema } from '@rjsf/utils'
import validator from '@rjsf/validator-ajv8'

import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { DescriptionFieldTemplate } from '@/components/rjsf/Templates/DescriptionFieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import type { ChakraRJSFormContext } from '@/components/rjsf/Form/types.ts'
import { customFormatsValidator } from '@/components/rjsf/Form/validation.utils.ts'
import { customFocusError } from '@/components/rjsf/Form/error-focus.utils.ts'
import { TitleFieldTemplate } from '@/components/rjsf/Templates/TitleFieldTemplate.tsx'
import { ErrorListTemplate } from '@/components/rjsf/Templates/ErrorListTemplate.tsx'
import { useFormControlStore } from '@/components/rjsf/Form/useFormControlStore.ts'
import { MqttTransformationField } from '@/components/rjsf/Fields'
import { adapterJSFWidgets } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import UpDownWidget from '@/components/rjsf/Widgets/UpDownWidget'

interface CustomFormProps<T>
  extends Pick<
    FormProps<T>,
    'id' | 'schema' | 'uiSchema' | 'formData' | 'formContext' | 'customValidate' | 'readonly' | 'onChange'
  > {
  onSubmit: (data: IChangeEvent) => void
  showNativeWidgets?: boolean
}

const FLAG_POST_VALIDATE = false

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const ChakraRJSForm: FC<CustomFormProps<any>> = ({
  id,
  schema,
  uiSchema,
  formData,
  onSubmit,
  onChange,
  formContext,
  customValidate,
  readonly,
  showNativeWidgets = false,
}) => {
  const { t } = useTranslation()
  const { setTabIndex } = useFormControlStore()
  const ref = useRef(null)
  const [batchData, setBatchData] = useState<JSONPatchDocument | undefined>(undefined)
  const defaultValues = useMemo(() => {
    if (batchData) {
      return immutableJSONPatch(formData, batchData)
    }
    return formData
  }, [batchData, formData])

  const onValidate = useCallback(
    (data: IChangeEvent<unknown>) => {
      onSubmit?.(data)
    },
    [onSubmit]
  )

  useEffect(
    () => {
      setTabIndex(0)
      return () => setTabIndex(0)
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
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
    focusOnError: customFocusError(ref),
  }

  const rjsfLog = debug(`RJSF:${id}`)
  // TODO[27657] Problem with the $schema property again; removing from the UI
  //   https://hivemq.kanbanize.com/ctrl_board/57/cards/27041/details/
  const { $schema, ...unspecifiedSchema } = schema

  return (
    <Form
      ref={ref}
      id={id}
      readonly={readonly}
      schema={unspecifiedSchema}
      uiSchema={showNativeWidgets ? undefined : uiSchema}
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
      widgets={{
        ...(!showNativeWidgets && adapterJSFWidgets),
        updown: UpDownWidget,
      }}
      fields={{
        ...(!showNativeWidgets && {
          'mqtt:transform': MqttTransformationField,
        }),
      }}
      onSubmit={onValidate}
      liveValidate
      // TODO[NVL] Removing HTML validation; see https://rjsf-team.github.io/react-jsonschema-form/docs/usage/validation/#html5-validation
      noHtml5Validate
      validator={customFormatsValidator}
      customValidate={customValidate}
      onError={(errors) => rjsfLog(t('error.rjsf.validation'), errors)}
      showErrorList="bottom"
      focusOnFirstError={context.focusOnError}
      onChange={onChange}
    />
  )
}

export default ChakraRJSForm
