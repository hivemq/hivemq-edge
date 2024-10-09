import { FC, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
import Form from '@rjsf/chakra-ui'
import debug from 'debug'

import { DomainTagList } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import { customFormatsValidator } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import { adapterJSFFields, adapterJSFWidgets } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import { ManagerContextType } from '@/modules/Mappings/types.ts'

interface DeviceTagFormProps {
  context: ManagerContextType
  onSubmit?: (data: DomainTagList | undefined) => void
}

const rjsfLog = debug('RJSF:DeviceTagForm')

const DeviceTagForm: FC<DeviceTagFormProps> = ({ context, onSubmit }) => {
  const { t } = useTranslation()

  const onFormSubmit = useCallback(
    (data: IChangeEvent<DomainTagList>) => {
      onSubmit?.(data.formData)
    },
    [onSubmit]
  )

  if (!context.schema) return <ErrorMessage message={t('device.errors.noFormSchema')} />

  return (
    <Form
      id="domainTags-instance-form"
      schema={context.schema}
      uiSchema={context.uiSchema}
      formData={context.formData}
      liveValidate
      noHtml5Validate
      focusOnFirstError
      onSubmit={onFormSubmit}
      validator={customFormatsValidator}
      showErrorList="bottom"
      widgets={adapterJSFWidgets}
      fields={adapterJSFFields}
      templates={{
        ObjectFieldTemplate,
        FieldTemplate,
        BaseInputTemplate,
        ArrayFieldTemplate,
        ArrayFieldItemTemplate,
      }}
      onError={(errors) => rjsfLog(t('error.rjsf.validation'), errors)}
    />
  )
}

export default DeviceTagForm
