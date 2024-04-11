import { FC, useMemo } from 'react'
import { useReactFlow } from 'reactflow'
import { useTranslation } from 'react-i18next'
import {
  Alert,
  AlertDescription,
  AlertIcon,
  AlertTitle,
  Box,
  Button,
  HStack,
  Icon,
  Stack,
  AlertStatus,
  CloseButton,
} from '@chakra-ui/react'

import PolicyErrorReport from '@datahub/components/helpers/PolicyErrorReport.tsx'
import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { DataHubNodeData, DesignerStatus, PolicyDryRunStatus } from '@datahub/types.ts'

export const ToolboxDryRun: FC = () => {
  const { t } = useTranslation('datahub')
  const { checkPolicyAsync } = usePolicyDryRun()
  const { fitView } = useReactFlow()
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
    nodes.forEach((node) => {
      onUpdateNodes<DataHubNodeData>(node.id, {
        ...node.data,
        dryRunStatus: PolicyDryRunStatus.IDLE,
      })
    })
  }

  const alertStatus: AlertStatus = status === PolicyDryRunStatus.SUCCESS ? 'success' : 'warning'
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
          <Alert status={alertStatus} data-testid="toolbox-policy-check-status">
            <AlertIcon />
            <Box whiteSpace="normal">
              <AlertTitle> {t('workspace.dryRun.report.success.title', { context: alertStatus })}</AlertTitle>
              <AlertDescription>
                {t('workspace.dryRun.report.success.description', { context: alertStatus })}
              </AlertDescription>
            </Box>
            <CloseButton alignSelf="flex-start" position="relative" right={-1} top={-1} onClick={handleClearPolicy} />
          </Alert>
          <PolicyErrorReport
            errors={getErrors() || []}
            onFitView={(id) => {
              const errorNode = nodes.find((node) => node.id === id)
              if (errorNode) fitView({ nodes: [errorNode], padding: 3, duration: 800 })
            }}
          />
        </>
      )}
    </Stack>
  )
}
