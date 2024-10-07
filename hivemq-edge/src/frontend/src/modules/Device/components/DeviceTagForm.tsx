import { FC, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { IChangeEvent } from '@rjsf/core'
import Form from '@rjsf/chakra-ui'

import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import { customFormatsValidator } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import { adapterJSFFields, adapterJSFWidgets } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import { useMappingManager } from '@/modules/Mappings/hooks/useMappingManager.tsx'

interface DeviceTagFormProps {
  adapterId: string
  adapterType?: string
}

const DeviceTagForm: FC<DeviceTagFormProps> = ({ adapterId, adapterType }) => {
  const { t } = useTranslation()
  const { tagsManager, isLoading } = useMappingManager(adapterId)
  const { data: tags, isError, isLoading: isLoadingTags } = useGetDomainTags(adapterId, adapterType)

  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      const subscriptions = data.formData?.subscriptions
      tagsManager?.onSubmit?.(subscriptions.tags)
    },
    [tagsManager]
  )

  if (isLoadingTags || isLoading) return <LoaderSpinner />
  if (!tagsManager || isError || tagsManager.errors)
    return <ErrorMessage message={t('protocolAdapter.error.loading')} />

  return (
    <Form
      id="adapter-instance-form"
      schema={tagsManager.schema}
      uiSchema={tagsManager.uiSchema}
      formData={{ tags: tags?.items }}
      liveValidate
      noHtml5Validate
      // focusOnFirstError
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
      onError={(errors) => console.log('XXXXX', errors)}
    />
  )
}

export default DeviceTagForm
