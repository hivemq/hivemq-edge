import { type FC, useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CustomValidator, FormContextType, RJSFSchema } from '@rjsf/utils'
import { IChangeEvent } from '@rjsf/core'

import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm.tsx'
import { MappingManagerType, MappingType } from '@/modules/Mappings/types.ts'
import { MappingContext } from '@/modules/ProtocolAdapters/types.ts'
import { customMappingValidate } from '@/modules/ProtocolAdapters/utils/validation-utils.ts'

interface MappingFormProps {
  adapterId: string
  adapterType?: string
  onSubmit: () => void
  useManager: (adapterId: string) => MappingManagerType
  type: MappingType
  showNativeWidgets?: boolean
}

const MappingForm: FC<MappingFormProps> = ({ adapterId, adapterType, useManager, type, showNativeWidgets = false }) => {
  const { t } = useTranslation()
  const { context, onUpdateCollection } = useManager(adapterId)
  const validationSchemas = useState<FlatJSONSchema7[]>()

  const onFormSubmit = useCallback(
    (data: IChangeEvent<unknown>) => {
      const promise = onUpdateCollection(data.formData)
      promise?.then(onSubmit)
    },
    [onSubmit, onUpdateCollection]
  )

  if (!context.schema) return <ErrorMessage message={t('protocolAdapter.export.error.noSchema')} />

  const contextExt: MappingContext = {
    isEditAdapter: true,
    isDiscoverable: false,
    adapterType: adapterType,
    adapterId: adapterId,
    validationSchemas,
  }

  return (
    <ChakraRJSForm
      showNativeWidgets={showNativeWidgets}
      id="adapter-mapping-form"
      schema={context.schema}
      uiSchema={context.uiSchema}
      formData={context.formData}
      formContext={contextExt}
      onSubmit={onFormSubmit}
      customValidate={
        type === MappingType.SOUTHBOUND && adapterType
          ? (customMappingValidate(adapterType) as CustomValidator<unknown, RJSFSchema, FormContextType>)
          : undefined
      }
    />
  )
}

export default MappingForm
