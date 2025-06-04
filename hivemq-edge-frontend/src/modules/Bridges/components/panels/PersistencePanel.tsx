import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Checkbox, FormControl, FormErrorMessage, FormHelperText } from '@chakra-ui/react'

import type { BridgePanelType } from '../../types.ts'

const PersistencePanel: FC<BridgePanelType> = ({ form }) => {
  const { t } = useTranslation()
  const {
    register,
    formState: { errors },
  } = form

  return (
    <FormControl variant="hivemq" flexGrow={1} display="flex" flexDirection="column" gap={4} as="fieldset">
      <FormControl isInvalid={!!errors.persist}>
        <Checkbox {...register('persist')}>{t('bridge.persistence.persist.label')}</Checkbox>
        <FormHelperText>{t('bridge.persistence.persist.helper')}</FormHelperText>
        <FormErrorMessage>{errors.persist && errors.persist.message}</FormErrorMessage>
      </FormControl>
    </FormControl>
  )
}

export default PersistencePanel
