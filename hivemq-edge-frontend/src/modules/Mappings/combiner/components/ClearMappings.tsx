import { Icon } from '@chakra-ui/react'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { RiDeleteBin2Fill } from 'react-icons/ri'

import type { DataCombining, Instruction } from '@/api/__generated__'
import IconButton from '@/components/Chakra/IconButton'

interface ClearMappingsProps {
  formData?: DataCombining
  onChange?: (instructions: Instruction[]) => void
}

export const ClearMappings: FC<ClearMappingsProps> = ({ onChange, formData }) => {
  const { t } = useTranslation()

  return (
    <IconButton
      isDisabled={!formData?.instructions?.length}
      icon={<Icon as={RiDeleteBin2Fill} />}
      aria-label={t('combiner.schema.mapping.action.clearMappings')}
      onClick={() => onChange?.([])}
    />
  )
}
