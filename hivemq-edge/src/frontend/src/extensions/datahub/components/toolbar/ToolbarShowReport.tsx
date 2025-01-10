import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
import { Icon } from '@chakra-ui/react'
import { RiPassportLine } from 'react-icons/ri'

import IconButton from '@/components/Chakra/IconButton.tsx'

import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'

export const ToolbarShowReport: FC = () => {
  const { t } = useTranslation('datahub')
  const { report } = usePolicyChecksStore()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  return (
    <IconButton
      icon={<Icon as={RiPassportLine} boxSize="24px" />}
      data-testid="node-toolbar-clear"
      aria-label={t('Show Validity Report')}
      isDisabled={!report}
      onClick={() => navigate('validation/', { state: { origin: pathname } })}
    />
  )
}
