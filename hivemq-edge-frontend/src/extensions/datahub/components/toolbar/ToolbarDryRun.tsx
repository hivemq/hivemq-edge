import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Icon } from '@chakra-ui/react'
import type { Node } from '@xyflow/react'

import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import type { DataPolicyData } from '@datahub/types.ts'
import { type BehaviorPolicyData, DataHubNodeType, PolicyDryRunStatus } from '@datahub/types.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

export const ToolbarDryRun: FC = () => {
  const { t } = useTranslation('datahub')
  const { checkPolicyAsync } = usePolicyDryRun()
  const { isPolicyEditable } = usePolicyGuards()
  const { status, node: selectedNode, initReport, setReport } = usePolicyChecksStore()
  const { nodes: allNodes } = useDataHubDraftStore()

  const CheckIcon = useMemo(() => getDryRunStatusIcon(status), [status])

  const handleCheckPolicy = () => {
    if (!selectedNode) return

    const target = allNodes.find(
      (node: Node): node is Node<BehaviorPolicyData> | Node<DataPolicyData> =>
        node.id === selectedNode.id &&
        (node.type === DataHubNodeType.DATA_POLICY || node.type === DataHubNodeType.BEHAVIOR_POLICY)
    )
    if (!target) return

    initReport()
    checkPolicyAsync(target).then((results): void => {
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
