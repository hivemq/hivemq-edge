import { FC, useMemo, useState } from 'react'
import { Button, ButtonGroup, Card, CardFooter, Icon, useToast } from '@chakra-ui/react'
import { NodeProps, useReactFlow } from 'reactflow'

import { RiPassExpiredLine, RiPassPendingLine, RiPassValidLine } from 'react-icons/ri'
import { MdOutlineDeleteForever } from 'react-icons/md'

import { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import CheckPolicyReportToast from '@datahub/components/helpers/CheckPolicyReportToast.tsx'
import { BehaviorPolicyData, DataHubNodeData, DataPolicyData, PolicyDryRunStatus } from '@datahub/types.ts'
import { usePolicyDryRun } from '@datahub/hooks/usePolicyDryRun.ts'
import { useTranslation } from 'react-i18next'

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

  const handleCheckPolicy = () => {
    const selectedNode = nodes.find((node) => node.id === props.node.id)
    if (!selectedNode) return

    setState(PolicyDryRunStatus.RUNNING)

    checkPolicyAsync(selectedNode).then((results) => {
      const failedResults = results.filter((result) => !!result.error)
      setState(failedResults.length ? PolicyDryRunStatus.FAILURE : PolicyDryRunStatus.SUCCESS)

      const errors = failedResults.map((result) => result.error as ProblemDetailsExtended)

      createToast({
        status: errors.length ? 'warning' : 'success',
        id: POLICY_CHECK_ID,
        isClosable: true,
        position: 'bottom-left',
        duration: null,
        variant: 'left-accent',
        onCloseComplete: () => {
          nodes.forEach((node) => {
            onUpdateNodes<DataHubNodeData>(node.id, {
              ...node.data,
              dryRunStatus: PolicyDryRunStatus.IDLE,
            })
          })
        },
        description: (
          <CheckPolicyReportToast
            errors={errors}
            onFitView={(id) => {
              const errorNode = nodes.find((e) => e.id === id)
              if (errorNode) fitView({ nodes: [errorNode], padding: 3, duration: 800 })
            }}
          />
        ),
      })
    })
  }

  const CheckIcon = useMemo(() => {
    if (state === PolicyDryRunStatus.IDLE) return RiPassPendingLine
    if (state === PolicyDryRunStatus.SUCCESS) return RiPassValidLine
    if (state === PolicyDryRunStatus.FAILURE) return RiPassExpiredLine
    return RiPassPendingLine
  }, [state])

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
