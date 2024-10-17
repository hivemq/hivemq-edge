import { type FC, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import Form from '@rjsf/chakra-ui'
import { type IChangeEvent } from '@rjsf/core'
import debug from 'debug'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import { ObjectFieldTemplate } from '@/components/rjsf/ObjectFieldTemplate.tsx'
import { FieldTemplate } from '@/components/rjsf/FieldTemplate.tsx'
import { BaseInputTemplate } from '@/components/rjsf/BaseInputTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'
import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'
import { customFormatsValidator } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import { MappingType } from '@/modules/Mappings/types.ts'
import { useMappingManager } from '@/modules/Mappings/hooks/useMappingManager.tsx'
import { adapterJSFFields, adapterJSFWidgets } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

interface MappingFormProps {
  adapterId: string
  adapterType?: string
  type: MappingType
}

const rjsfLog = debug('RJSF:MappingForm')

// TODO[NVL] Should replicate the config from the adapter form; share component?
const MappingForm: FC<MappingFormProps> = ({ adapterId, adapterType, type }) => {
  const { t } = useTranslation()
  const { inwardManager, outwardManager } = useMappingManager(adapterId)
  const { errorToast } = useEdgeToast()

  const mappingManager = type === MappingType.INWARD ? inwardManager : outwardManager

  const context: AdapterContext = {
    isEditAdapter: true,
    isDiscoverable: false,
    adapterType: adapterType,
    adapterId: adapterId,
  }

  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      if (!mappingManager?.onSubmit) {
        errorToast(
          {
            title: t('protocolAdapter.toast.update.title'),
            description: t('protocolAdapter.toast.update.error'),
          },
          new Error(t('protocolAdapter.export.error.noSchema'))
        )
      } else mappingManager.onSubmit(data.formData)
    },
    [errorToast, mappingManager, t]
  )

  if (!mappingManager) return <ErrorMessage message={t('protocolAdapter.export.error.noSchema')} />

  return (
    <Form
      id="adapter-instance-form"
      schema={mappingManager.schema}
      uiSchema={mappingManager.uiSchema}
      formData={mappingManager.formData}
      formContext={context}
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

export default MappingForm
