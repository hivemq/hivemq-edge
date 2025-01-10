import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Icon } from '@chakra-ui/react'

import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { DesignerStatus, PolicyDryRunStatus } from '@datahub/types.ts'

export const ToolbarDryRun: FC = () => {
  const { t } = useTranslation('datahub')
  const { checkPolicyAsync } = usePolicyDryRun()

  const { status: statusDraft } = useDataHubDraftStore()
  const { status, node: selectedNode, initReport, setReport } = usePolicyChecksStore()

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

  return (
    <Button
      data-testid="toolbox-policy-check"
      leftIcon={<Icon as={CheckIcon} boxSize="24px" />}
      isLoading={status === PolicyDryRunStatus.RUNNING}
      loadingText={t('workspace.dryRun.toolbar.checking')}
      onClick={handleCheckPolicy}
      isDisabled={!selectedNode || !isEditEnabled}
      data-status={status}
    >
      {t('workspace.toolbar.policy.check')}
    </Button>
  )
}
