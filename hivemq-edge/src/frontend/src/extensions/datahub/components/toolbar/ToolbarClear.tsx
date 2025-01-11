import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Icon } from '@chakra-ui/react'
import { CloseIcon } from '@chakra-ui/icons'

import IconButton from '@/components/Chakra/IconButton.tsx'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { DataHubNodeData, PolicyDryRunStatus } from '@datahub/types.ts'

export const ToolbarClear: FC = () => {
  const { t } = useTranslation('datahub')

  const { nodes, onUpdateNodes } = useDataHubDraftStore()
  const { node: selectedNode, setNode, reset, report } = usePolicyChecksStore()

  const handleClearPolicy = () => {
    reset()
    setNode(selectedNode)
    nodes.forEach((node) => {
      onUpdateNodes<DataHubNodeData>(node.id, {
        ...node.data,
        dryRunStatus: PolicyDryRunStatus.IDLE,
      })
    })
  }

  return (
    <IconButton
      icon={<Icon as={CloseIcon} boxSize="12px" />}
      data-testid="toolbox-policy-clear"
      aria-label={t('workspace.toolbar.policy.clearReport')}
      onClick={handleClearPolicy}
      isDisabled={!report}
    />
  )
}
