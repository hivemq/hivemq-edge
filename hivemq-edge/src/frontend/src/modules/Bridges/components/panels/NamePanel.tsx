import { FC } from 'react'
import { FormControl, FormErrorMessage, FormHelperText, FormLabel, Input } from '@chakra-ui/react'

import { useTranslation } from 'react-i18next'
import { BridgePanelType } from '@/modules/Bridges/types.ts'
import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.tsx'
import { $Bridge } from '@/api/__generated__'

const NamePanel: FC<BridgePanelType> = ({ form, isNewBridge = false }) => {
  const { t } = useTranslation()
  const {
    register,
    formState: { errors: validationErrors },
  } = form
  const { data } = useListBridges()

  return (
    <FormControl isInvalid={!!validationErrors.id} isRequired={isNewBridge}>
      <FormLabel htmlFor="name">{t('bridge.options.id.label')}</FormLabel>
      <Input
        isDisabled={!isNewBridge}
        autoFocus
        id="name"
        type="text"
        autoComplete={'name'}
        placeholder={t('bridge.options.id.placeholder') as string}
        {...register('id', {
          required: {
            value: $Bridge.properties.id.isRequired,
            message: t('bridge.options.id.error.required') as string,
          },
          pattern: { value: /^[a-zA-Z0-9_-]+$/, message: t('bridge.options.id.error.pattern') as string },
          validate: {
            notUnique: (value) => {
              if (!isNewBridge) return true
              const isIncluded = data?.map((e) => e.id).includes(value)
              return !isIncluded || (t('bridge.options.id.error.notUnique') as string)
            },
          },
        })}
      />
      {!validationErrors.id && <FormHelperText>{t('bridge.options.id.helper')}</FormHelperText>}
      <FormErrorMessage>{validationErrors.id && validationErrors.id.message}</FormErrorMessage>
    </FormControl>
  )
}

export default NamePanel
