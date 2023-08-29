import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { FormControl, FormErrorMessage, FormLabel, Select } from '@chakra-ui/react'

import { ProtocolAdapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.tsx'
import { AdapterType, GenericPanelType } from '@/modules/ProtocolAdapters/types.ts'

import AdapterTypeSummary from '../adapters/AdapterTypeSummary.tsx'

const AdapterTypeSelector: FC<GenericPanelType<AdapterType>> = ({ form }) => {
  const { t } = useTranslation()
  const {
    register,
    watch,
    formState: { errors },
  } = form
  const { data } = useGetAdapterTypes()
  const selectedType = watch('adapterType')
  const selectedAdapter = data?.items?.find((e) => e.id === selectedType)

  return (
    <FormControl isInvalid={!!errors.adapterType} isRequired>
      <FormLabel htmlFor="clientId">{t('protocolAdapter.type.label')}</FormLabel>
      <Select
        id="clientId"
        placeholder={t('protocolAdapter.type.select') as string}
        {...register('adapterType', {
          required: 'This field is required',
        })}
      >
        {data?.items?.map((type: ProtocolAdapter) => (
          <option key={type.id} value={type.id}>
            {type.name}
          </option>
        ))}
      </Select>

      {selectedAdapter && <AdapterTypeSummary adapter={selectedAdapter} />}

      <FormErrorMessage>{errors.adapterType && errors.adapterType.message}</FormErrorMessage>
    </FormControl>
  )
}

export default AdapterTypeSelector
