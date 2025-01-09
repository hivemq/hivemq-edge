import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Icon } from '@chakra-ui/react'
import { CloseIcon } from '@chakra-ui/icons'

import IconButton from '@/components/Chakra/IconButton.tsx'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'

import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { DataHubNodeData, DesignerStatus, PolicyDryRunStatus } from '@datahub/types.ts'

export const ToolbarDryRun: FC = () => {
  const { t } = useTranslation('datahub')
  const { checkPolicyAsync } = usePolicyDryRun()

  const { nodes, onUpdateNodes, status: statusDraft } = useDataHubDraftStore()
  const { status, node: selectedNode, report, initReport, setReport, setNode, reset } = usePolicyChecksStore()

  const CheckIcon = useMemo(() => getDryRunStatusIcon(status), [status])
  const isEditEnabled =
    import.meta.env.VITE_FLAG_DATAHUB_EDIT_POLICY_ENABLED === 'true' || statusDraft === DesignerStatus.DRAFT

  const handleCheckPolicy = () => {
    if (!selectedNode) return

    initReport()
    checkPolicyAsync(selectedNode).then((results): void => {
      setReport(results)
    })
  }

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
    <ToolbarButtonGroup variant="outline">
      <Button
        data-testid="toolbox-policy-check"
        leftIcon={<Icon as={CheckIcon} boxSize="24px" />}
        isLoading={status === PolicyDryRunStatus.RUNNING}
        loadingText={t('workspace.dryRun.toolbar.checking')}
        onClick={handleCheckPolicy}
        isDisabled={!selectedNode || !isEditEnabled}
      >
        {t('workspace.toolbar.policy.check')}
      </Button>
      {report && (
        <IconButton
          icon={<Icon as={CloseIcon} boxSize="12px" />}
          data-testid="node-toolbar-delete"
          aria-label={t('Clear Validity Report')}
          onClick={handleClearPolicy}
        />
      )}
    </ToolbarButtonGroup>
  )
}
