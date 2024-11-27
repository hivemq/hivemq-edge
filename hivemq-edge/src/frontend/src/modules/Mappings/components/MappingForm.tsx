import { type FC, useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { type IChangeEvent } from '@rjsf/core'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm.tsx'
import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'
import { customMappingValidate } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'
import { MappingType } from '@/modules/Mappings/types.ts'
import { useMappingManager } from '@/modules/Mappings/hooks/useMappingManager.tsx'
import { MappingContext } from '@/modules/ProtocolAdapters/types.ts'
import { CustomValidator, RJSFSchema } from '@rjsf/utils'
import { FormContextType } from '@rjsf/utils/src/types.ts'

interface MappingFormProps {
  adapterId: string
  adapterType?: string
  type: MappingType
  onSubmit: () => void
}

const MappingForm: FC<MappingFormProps> = ({ adapterId, adapterType, type, onSubmit }) => {
  const { t } = useTranslation()
  const { inwardManager, outwardManager } = useMappingManager(adapterId)
  const { errorToast } = useEdgeToast()
  const validationSchemas = useState<FlatJSONSchema7[]>()
  const mappingManager = type === MappingType.INWARD ? inwardManager : outwardManager

  const context: MappingContext = {
    isEditAdapter: true,
    isDiscoverable: false,
    adapterType: adapterType,
    adapterId: adapterId,
    validationSchemas,
  }

  const onFormSubmit = useCallback(
    (data: IChangeEvent<unknown>) => {
      if (!mappingManager?.onSubmit) {
        errorToast(
          {
            title: t('protocolAdapter.toast.update.title'),
            description: t('protocolAdapter.toast.update.error'),
          },
          new Error(t('protocolAdapter.export.error.noSchema'))
        )
      } else {
        mappingManager.onSubmit(data.formData)
        // TODO[NVL] A bit too fast; handle pending and errors
        onSubmit()
      }
    },
    [errorToast, mappingManager, onSubmit, t]
  )

  if (!mappingManager) return <ErrorMessage message={t('protocolAdapter.export.error.noSchema')} />

  return (
    <ChakraRJSForm
      id="adapter-mapping-form"
      schema={mappingManager.schema}
      uiSchema={mappingManager.uiSchema}
      formData={mappingManager.formData}
      formContext={context}
      onSubmit={onFormSubmit}
      customValidate={
        type === MappingType.OUTWARD && adapterType
          ? (customMappingValidate(adapterType) as CustomValidator<unknown, RJSFSchema, FormContextType>)
          : undefined
      }
    />
  )
}

export default MappingForm
