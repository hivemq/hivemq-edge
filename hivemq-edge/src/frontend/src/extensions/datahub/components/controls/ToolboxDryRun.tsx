import { FC, useCallback, useMemo } from 'react'
import { useReactFlow } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
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
  Link,
  Text,
} from '@chakra-ui/react'

import { ConditionalWrapper } from '@/components/ConditonalWrapper.tsx'

import PolicyErrorReport from '@datahub/components/helpers/PolicyErrorReport.tsx'
import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'
import { DataHubNodeData, DesignerStatus, PolicyDryRunStatus } from '@datahub/types.ts'
import { DesignerToolBoxProps } from '@datahub/components/controls/DesignerToolbox.tsx'

export const ToolboxDryRun: FC<DesignerToolBoxProps> = ({ onActiveStep }) => {
  const { t } = useTranslation('datahub')
  const { checkPolicyAsync } = usePolicyDryRun()
  const { fitView } = useReactFlow()
  const navigate = useNavigate()
  const { pathname } = useLocation()
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
    setNode(selectedNode)
    nodes.forEach((node) => {
      onUpdateNodes<DataHubNodeData>(node.id, {
        ...node.data,
        dryRunStatus: PolicyDryRunStatus.IDLE,
      })
    })
  }
  const errorNodeFrom = useCallback((id: string) => nodes.find((node) => node.id === id), [nodes])
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
              <AlertTitle>
                <Text as="span">{t('workspace.dryRun.report.success.title', { context: alertStatus })}</Text>
              </AlertTitle>
              <AlertDescription>
                <ConditionalWrapper
                  condition={status === PolicyDryRunStatus.SUCCESS}
                  wrapper={(children) => (
                    <Link
                      aria-label={t('workspace.toolbox.navigation.goPublish') as string}
                      onClick={() => onActiveStep?.(2)}
                    >
                      {children}{' '}
                    </Link>
                  )}
                >
                  <Text as="span">{t('workspace.dryRun.report.success.description', { context: alertStatus })}</Text>
                </ConditionalWrapper>
              </AlertDescription>
            </Box>
            <CloseButton alignSelf="flex-start" position="relative" right={-1} top={-1} onClick={handleClearPolicy} />
          </Alert>
          <PolicyErrorReport
            errors={getErrors() || []}
            onFitView={(id) => {
              const errorNode = errorNodeFrom(id)
              if (errorNode) fitView({ nodes: [errorNode], padding: 3, duration: 800 })
            }}
            onOpenConfig={(id) => {
              const errorNode = errorNodeFrom(id)
              if (errorNode) navigate(`node/${errorNode.type}/${id}`, { state: { origin: pathname } })
            }}
          />
        </>
      )}
    </Stack>
  )
}
