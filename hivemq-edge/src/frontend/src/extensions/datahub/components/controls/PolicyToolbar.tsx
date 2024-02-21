import { FC, useMemo, useState } from 'react'
import { NodeProps, useReactFlow } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { Button, ButtonGroup, Card, CardFooter, Icon, useToast, UseToastOptions } from '@chakra-ui/react'
import { MdOutlineDeleteForever, MdOutlineSave } from 'react-icons/md'

import { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import PolicyErrorReport from '@datahub/components/helpers/PolicyErrorReport.tsx'
import { BehaviorPolicyData, DataHubNodeData, DataPolicyData, PolicyDryRunStatus } from '@datahub/types.ts'
import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import { getDryRunStatusIcon } from '@datahub/utils/node.utils.ts'

interface ActionPolicyCheckProps {
  node: NodeProps<DataPolicyData | BehaviorPolicyData>
}

export const PolicyToolbar: FC<ActionPolicyCheckProps> = (props) => {
  const { t } = useTranslation('datahub')
  const [state, setState] = useState<PolicyDryRunStatus>(PolicyDryRunStatus.IDLE)
  const store = useDataHubDraftStore()
  const { nodes, onUpdateNodes } = store
  const createToast = useToast()
  const { fitView } = useReactFlow()
  const { checkPolicyAsync } = usePolicyDryRun()

  // only allow a single check at any given time
  const POLICY_CHECK_ID = 'POLICY_CHECK_ID'

  const TOAST_DEFAULTS: UseToastOptions = {
    id: POLICY_CHECK_ID,
    isClosable: true,
    position: 'bottom-left',
    variant: 'left-accent',
    duration: null,
  }

  const handleCheckPolicy = () => {
    const selectedNode = nodes.find((node) => node.id === props.node.id)
    if (!selectedNode) return

    setState(PolicyDryRunStatus.RUNNING)

    if (!createToast.isActive(POLICY_CHECK_ID)) {
      createToast.promise(checkPolicyAsync(selectedNode), {
        success: (results) => {
          setState(PolicyDryRunStatus.SUCCESS)
          const failedResults = results.filter((result) => !!result.error)
          const errors = failedResults.map((result) => result.error as ProblemDetailsExtended)
          const status = errors.length ? 'warning' : 'success'

          return {
            title: t('workspace.dryRun.report.success.title', { context: status }),
            status: status,
            description: (
              <PolicyErrorReport
                errors={errors}
                onFitView={(id) => {
                  const errorNode = nodes.find((e) => e.id === id)
                  if (errorNode) fitView({ nodes: [errorNode], padding: 3, duration: 800 })
                }}
              />
            ),
            ...TOAST_DEFAULTS,
            onCloseComplete: () => {
              nodes.forEach((node) => {
                onUpdateNodes<DataHubNodeData>(node.id, {
                  ...node.data,
                  dryRunStatus: PolicyDryRunStatus.IDLE,
                })
              })
            },
          }
        },
        error: (e) => {
          setState(PolicyDryRunStatus.FAILURE)
          return { title: t('workspace.dryRun.report.error.title'), description: e.toString(), ...TOAST_DEFAULTS }
        },
        loading: {
          title: t('workspace.dryRun.report.loading.title'),
          description: t('workspace.dryRun.report.loading.description'),
          ...TOAST_DEFAULTS,
          isClosable: false,
        },
      })
    }
  }

  const CheckIcon = useMemo(() => getDryRunStatusIcon(state), [state])

  return (
    <Card boxShadow="dark-lg" rounded="md" bg="white" transform="translate(0,-24px)">
      <CardFooter gap={2} p={2}>
        <ButtonGroup size="md">
          <Button
            leftIcon={<Icon as={CheckIcon} boxSize="24px" />}
            isLoading={state === PolicyDryRunStatus.RUNNING}
            loadingText={t('workspace.dryRun.toolbar.checking')}
            onClick={handleCheckPolicy}
            isDisabled={createToast.isActive(POLICY_CHECK_ID)}
          >
            {t('workspace.toolbar.policy.check')}
          </Button>
        </ButtonGroup>
        <ButtonGroup size="md">
          <Button leftIcon={<MdOutlineSave />} isDisabled>
            {t('workspace.toolbar.policy.save')}
          </Button>
          <Button leftIcon={<MdOutlineDeleteForever />} variant="danger" isDisabled>
            {t('workspace.toolbar.policy.delete')}
          </Button>
        </ButtonGroup>
      </CardFooter>
    </Card>
  )
}
