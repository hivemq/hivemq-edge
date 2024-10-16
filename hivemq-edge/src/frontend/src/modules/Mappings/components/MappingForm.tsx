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

  const mappingManager = type === MappingType.INWARD ? inwardManager : outwardManager

  const context: AdapterContext = {
    isEditAdapter: true,
    isDiscoverable: false,
    adapterType: adapterType,
    adapterId: adapterId,
  }

  /**
   * @deprecated This is a mock, missing validation (https://hivemq.kanbanize.com/ctrl_board/57/cards/25908/details/)
   */
  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      const subscriptions = data.formData?.subscriptions
      mappingManager?.onSubmit?.(subscriptions)
    },
    [mappingManager]
  )

  if (!mappingManager) return <ErrorMessage message={t('protocolAdapter.export.error.noSchema')} />

  return (
    <Form
      id="adapter-instance-form"
      schema={mappingManager.schema}
      uiSchema={mappingManager.uiSchema}
      liveValidate
      onSubmit={onFormSubmit}
      validator={customFormatsValidator}
      showErrorList="bottom"
      onError={(errors) => rjsfLog(t('error.rjsf.validation'), errors)}
      formData={mappingManager.formData}
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
