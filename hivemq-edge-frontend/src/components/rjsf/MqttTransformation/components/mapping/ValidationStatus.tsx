import type { FC } from 'react'
import { Alert, AlertIcon, AlertTitle } from '@chakra-ui/react'
import type { MappingValidation } from '@/modules/Mappings/types.ts'
import { useTranslation } from 'react-i18next'

interface ValidationStatusProps {
  validation: MappingValidation
}

const ValidationStatus: FC<ValidationStatusProps> = ({ validation }) => {
  const { t } = useTranslation('components')
  return (
    <Alert status={validation.status} size="xs" p={1} width="inherit">
      <AlertIcon />
      <AlertTitle>{t('rjsf.MqttTransformationField.status.title', { context: validation.status })}</AlertTitle>
    </Alert>
  )
}

export default ValidationStatus
