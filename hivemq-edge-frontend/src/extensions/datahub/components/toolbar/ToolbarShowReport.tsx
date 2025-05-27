import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button, Icon } from '@chakra-ui/react'
import { RiPassportLine } from 'react-icons/ri'

import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'

export const ToolbarShowReport: FC = () => {
  const { t } = useTranslation('datahub')
  const { report } = usePolicyChecksStore()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  return (
    <Button
      leftIcon={<Icon as={RiPassportLine} boxSize="24px" />}
      data-testid="toolbox-policy-report"
      isDisabled={!report}
      onClick={() => navigate('validation/', { state: { origin: pathname } })}
    >
      {t('workspace.toolbar.policy.showReport')}
    </Button>
  )
}
