import { FC, useMemo } from 'react'
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
import { DataHubNodeData, PolicyDryRunStatus } from '@datahub/types.ts'
import { useTranslation } from 'react-i18next'
import { getDryRunStatusIcon, isBehaviorPolicyNodeType, isDataPolicyNodeType } from '@datahub/utils/node.utils.ts'
import { useOnSelectionChange, useReactFlow } from 'reactflow'
import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import PolicyErrorReport from '@datahub/components/helpers/PolicyErrorReport.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'

const ToolboxDryRun: FC = () => {
  const { t } = useTranslation('datahub')
  const { checkPolicyAsync } = usePolicyDryRun()
  const { fitView } = useReactFlow()
  const { nodes, onUpdateNodes } = useDataHubDraftStore()
  const { status, node, report, setNode, initReport, setReport, getErrors, reset } = usePolicyChecksStore()

  const CheckIcon = useMemo(() => getDryRunStatusIcon(status), [status])

  useOnSelectionChange({
    onChange: ({ nodes }) => {
      if (nodes.length === 1) {
        const [node] = nodes
        if (isDataPolicyNodeType(node) || isBehaviorPolicyNodeType(node)) setNode(node)
      } else if (nodes.length === 0) setNode(undefined)
    },
  })

  const handleCheckPolicy = () => {
    if (!node) return

    initReport()
    checkPolicyAsync(node).then((results): void => {
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
            leftIcon={<Icon as={CheckIcon} boxSize="24px" />}
            isLoading={status === PolicyDryRunStatus.RUNNING}
            loadingText={t('workspace.dryRun.toolbar.checking')}
            onClick={handleCheckPolicy}
            isDisabled={!node || status === PolicyDryRunStatus.SUCCESS || status === PolicyDryRunStatus.FAILURE}
          >
            {t('workspace.toolbar.policy.check')}
          </Button>
        </Box>
      </HStack>

      {report && (
        <>
          <Alert status={alertStatus}>
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

export default ToolboxDryRun
