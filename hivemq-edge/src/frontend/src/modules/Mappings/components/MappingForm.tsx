import { type FC, useCallback } from 'react'
import Form from '@rjsf/chakra-ui'
import { type IChangeEvent } from '@rjsf/core'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import { customFormatsValidator } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import { adapterJSFFields, adapterJSFWidgets } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import { useMappingManager } from '@/modules/Mappings/hooks/useMappingManager.tsx'
import { useTranslation } from 'react-i18next'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

interface MappingFormProps {
  adapterId: string
  adapterType?: string
  type: 'inward' | 'outward'
}

// TODO[NVL] Should replicate the config from the adapter form; share component?
const MappingForm: FC<MappingFormProps> = ({ adapterId, adapterType, type }) => {
  const { t } = useTranslation()
  const { inwardManager, outwardManager } = useMappingManager(adapterId)

  const subscriptionManager = type === 'inward' ? inwardManager : outwardManager

  const context: AdapterContext = {
    isEditAdapter: true,
    isDiscoverable: false,
    adapterType: adapterType,
    adapterId: adapterId,
  }

  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      const subscriptions = data.formData?.subscriptions
      subscriptionManager?.onSubmit?.(subscriptions)
    },
    [subscriptionManager]
  )

  if (!subscriptionManager) return <ErrorMessage type={type} message={t('protocolAdapter.export.error.noSchema')} />

  return (
    <Form
      id="adapter-instance-form"
      schema={subscriptionManager.schema}
      uiSchema={subscriptionManager.uiSchema}
      liveValidate
      onSubmit={onFormSubmit}
      validator={customFormatsValidator}
      showErrorList="bottom"
      onError={(errors) => console.log('XXXXXXX', errors)}
      formData={subscriptionManager.formData}
      widgets={adapterJSFWidgets}
      fields={adapterJSFFields}
      formContext={context}
      templates={{
        ObjectFieldTemplate,
        FieldTemplate,
        BaseInputTemplate,
        ArrayFieldTemplate,
        ArrayFieldItemTemplate,
      }}
    />
  )
}

export default MappingForm
