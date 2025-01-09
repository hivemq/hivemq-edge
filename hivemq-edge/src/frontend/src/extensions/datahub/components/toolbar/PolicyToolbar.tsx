import { FC } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Icon } from '@chakra-ui/react'
import { RiPassportLine } from 'react-icons/ri'
import { useTranslation } from 'react-i18next'

import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'

import { ToolbarDryRun } from '@datahub/components/toolbar/ToolbarDryRun.tsx'
import { ToolbarPublish } from '@datahub/components/toolbar/ToolbarPublish.tsx'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'

interface PolicyToolbarProps {
  id?: string
}

const PolicyToolbar: FC<PolicyToolbarProps> = () => {
  const { t } = useTranslation('datahub')
  const { report } = usePolicyChecksStore()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  return (
    <ToolbarButtonGroup orientation="horizontal" variant="outline" gap="0.5em">
      <ToolbarDryRun />
      <ToolbarButtonGroup orientation="horizontal" variant="outline">
        <IconButton
          icon={<Icon as={RiPassportLine} boxSize="24px" />}
          data-testid="node-toolbar-delete"
          aria-label={t('Show Validity Report')}
          isDisabled={!report}
          onClick={() => navigate('validation/', { state: { origin: pathname } })}
        />
      </ToolbarButtonGroup>
      <ToolbarPublish />
    </ToolbarButtonGroup>
  )
}

export default PolicyToolbar
