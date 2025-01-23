import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Icon } from '@chakra-ui/react'

import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { PolicyDryRunStatus } from '@datahub/types.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

export const ToolbarDryRun: FC = () => {
  const { t } = useTranslation('datahub')
  const { checkPolicyAsync } = usePolicyDryRun()
  const { isPolicyEditable } = usePolicyGuards()
  const { status, node: selectedNode, initReport, setReport } = usePolicyChecksStore()

  const CheckIcon = useMemo(() => getDryRunStatusIcon(status), [status])

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
      isDisabled={!selectedNode || !isPolicyEditable}
      data-status={status}
    >
      {t('workspace.toolbar.policy.check')}
    </Button>
  )
}
