import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertIcon, Checkbox, FormControl, FormErrorMessage, FormHelperText } from '@chakra-ui/react'

import { Capability } from '@/api/__generated__'
import { BridgePanelType } from '../../types.ts'

interface PersistencePanelType extends BridgePanelType {
  hasPersistence: Capability
}

const PersistencePanel: FC<PersistencePanelType> = ({ form }) => {
  const { t } = useTranslation()
  const {
    register,
    formState: { errors, dirtyFields },
  } = form

  return (
    <>
      {dirtyFields.persist && (
        <Alert status="info" mb={4}>
          <AlertIcon />
          {t('bridge.persistence.restartWarning')}
        </Alert>
      )}
      <FormControl variant={'hivemq'} flexGrow={1} display={'flex'} flexDirection={'column'} gap={4} as={'fieldset'}>
        <FormControl isInvalid={!!errors.persist}>
          <Checkbox {...register('persist')}>{t('bridge.persistence.persist.label')}</Checkbox>
          <FormHelperText>{t('bridge.persistence.persist.helper')}</FormHelperText>
          <FormErrorMessage>{errors.persist && errors.persist.message}</FormErrorMessage>
        </FormControl>
      </FormControl>
    </>
  )
}

export default PersistencePanel
