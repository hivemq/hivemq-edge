import { FC, useCallback, useMemo } from 'react'
import { Node } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { Box, Button, HStack, Icon, Stack } from '@chakra-ui/react'

import config from '@/config'

import PolicyErrorReport from '@datahub/components/helpers/PolicyErrorReport.tsx'
import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { DataHubNodeData, DesignerStatus, PolicyDryRunStatus } from '@datahub/types.ts'
import { DesignerToolBoxProps } from '@datahub/components/controls/DesignerToolbox.tsx'
import PolicySummaryReport from '@datahub/components/helpers/PolicySummaryReport.tsx'

interface ToolboxDryRunProps extends DesignerToolBoxProps {
  onShowNode?: (node: Node) => void
  onShowEditor?: (node: Node) => void
}

export const ToolboxDryRun: FC<ToolboxDryRunProps> = ({ onActiveStep, onShowNode, onShowEditor }) => {
  const { t } = useTranslation('datahub')
  const { checkPolicyAsync } = usePolicyDryRun()

  const { nodes, onUpdateNodes, status: statusDraft } = useDataHubDraftStore()
  const {
    status,
    node: selectedNode,
    report,
    initReport,
    setReport,
    setNode,
    getErrors,
    reset,
  } = usePolicyChecksStore()

  const CheckIcon = useMemo(() => getDryRunStatusIcon(status), [status])
  const isEditEnabled = config.features.DATAHUB_EDIT_POLICY_ENABLED || statusDraft === DesignerStatus.DRAFT

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
  const errorNodeFrom = useCallback((id: string) => nodes.find((node) => node.id === id), [nodes])

  return (
    <Stack maxW={500}>
      <HStack>
        <Box>
          <Button
            data-testid="toolbox-policy-check"
            leftIcon={<Icon as={CheckIcon} boxSize="24px" />}
            isLoading={status === PolicyDryRunStatus.RUNNING}
            loadingText={t('workspace.dryRun.toolbar.checking')}
            onClick={handleCheckPolicy}
            isDisabled={
              !selectedNode ||
              status === PolicyDryRunStatus.SUCCESS ||
              status === PolicyDryRunStatus.FAILURE ||
              !isEditEnabled
            }
          >
            {t('workspace.toolbar.policy.check')}
          </Button>
        </Box>
      </HStack>

      {report && (
        <>
          <PolicySummaryReport
            status={status || PolicyDryRunStatus.FAILURE}
            onOpenPublish={() => onActiveStep?.(DesignerToolBoxProps.Steps.TOOLBOX_PUBLISH)}
            onClearPolicy={handleClearPolicy}
          />
          <PolicyErrorReport
            errors={getErrors() || []}
            onFitView={(id) => {
              const errorNode = errorNodeFrom(id)
              if (errorNode && onShowNode) onShowNode(errorNode)
            }}
            onOpenConfig={(id) => {
              const errorNode = errorNodeFrom(id)
              if (errorNode && onShowEditor) onShowEditor(errorNode)
            }}
          />
        </>
      )}
    </Stack>
  )
}
