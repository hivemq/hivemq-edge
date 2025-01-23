import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import type { UseMutateAsyncFunction } from '@tanstack/react-query'
import { Button, Icon, useToast } from '@chakra-ui/react'
import { MdPublishedWithChanges } from 'react-icons/md'

import type { BehaviorPolicy, DataPolicy, PolicySchema, Script } from '@/api/__generated__'

import { useCreateDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/useCreateDataPolicy.tsx'
import { useCreateBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useCreateBehaviorPolicy.tsx'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { useCreateSchema } from '@datahub/api/hooks/DataHubSchemasService/useCreateSchema.tsx'
import { useCreateScript } from '@datahub/api/hooks/DataHubScriptsService/useCreateScript.tsx'
import { dataHubToastOption } from '@datahub/utils/toast.utils.ts'
import type { DryRunResults, ResourceState } from '@datahub/types.ts'
import { DataHubNodeType, ResourceWorkingVersion } from '@datahub/types.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

interface Mutate<T> {
  type: DataHubNodeType
  payload: T
  mutation: UseMutateAsyncFunction<T, unknown, T>
}
type ValidMutate = Mutate<PolicySchema> | Mutate<Script> | Mutate<DataPolicy> | Mutate<BehaviorPolicy>

const resourceReducer =
  <T extends PolicySchema | Script>(type: DataHubNodeType) =>
  (accumulator: T[], result: DryRunResults<T, never>) => {
    if (result.node.type !== type) return accumulator
    if (!result.data) return accumulator
    const { id } = result.data
    if (!id) return accumulator

    const { version } = result.node.data as ResourceState
    if (version !== ResourceWorkingVersion.DRAFT && version !== ResourceWorkingVersion.MODIFIED) return accumulator

    const allIds = accumulator.map((resource) => resource.id)
    if (allIds.includes(id)) return accumulator

    accumulator.push(result.data)
    return accumulator
  }

export const ToolbarPublish: FC = () => {
  const { t } = useTranslation('datahub')
  const { report, node: selectedNode, setNode, reset } = usePolicyChecksStore()
  const createSchema = useCreateSchema()
  const createScript = useCreateScript()
  const createDataPolicy = useCreateDataPolicy()
  const createBehaviorPolicy = useCreateBehaviorPolicy()
  const toast = useToast()
  const navigate = useNavigate()
  const { isPolicyEditable } = usePolicyGuards()

  const isValid = !!report && report.length >= 1 && report?.every((e) => !e.error)

  const handleMutation = async (promise: Promise<ValidMutate>, type: DataHubNodeType) => {
    try {
      await promise
      toast({
        ...dataHubToastOption,
        title: t('error.publish.title', { source: type }),
        description: t('error.publish.description', { source: type }),
        status: 'success',
      })
    } catch (error) {
      let message
      if (error instanceof Error) message = error.message
      else message = String(error)
      toast({
        ...dataHubToastOption,
        title: t('error.publish.error', { source: type }),
        description: message,
        status: 'error',
      })
    }
  }

  const handlePublish = () => {
    if (!report) return

    const payload = [...report].pop()
    if (!payload) return

    const { data, resources } = payload

    const allSchemas =
      resources?.reduce(resourceReducer<PolicySchema>(DataHubNodeType.SCHEMA), []).map<Mutate<PolicySchema>>((e) => ({
        type: DataHubNodeType.SCHEMA,
        payload: e,
        mutation: createSchema.mutateAsync,
      })) || []

    const allScripts =
      resources?.reduce(resourceReducer<Script>(DataHubNodeType.FUNCTION), []).map<Mutate<Script>>((e) => ({
        type: DataHubNodeType.FUNCTION,
        payload: e,
        mutation: createScript.mutateAsync,
      })) || []

    const promises = [...allSchemas, ...allScripts].map((request) => {
      return handleMutation(
        // @ts-ignore TODO[NVL] Weird! Check the type mismatch
        request.mutation(request.payload),
        request.type
      )
    })

    Promise.all(promises)
      .then(() => {
        if (payload.node.type === DataHubNodeType.DATA_POLICY) {
          const policy: Mutate<DataPolicy> = {
            type: DataHubNodeType.DATA_POLICY,
            payload: data as DataPolicy,
            mutation: createDataPolicy.mutateAsync,
          }
          handleMutation(
            // @ts-ignore TODO[NVL] Weird! Check the type mismatch
            policy.mutation(policy.payload),
            DataHubNodeType.DATA_POLICY
          )
        } else if (payload.node.type === DataHubNodeType.BEHAVIOR_POLICY) {
          const policy: Mutate<BehaviorPolicy> = {
            type: DataHubNodeType.BEHAVIOR_POLICY,
            payload: data as BehaviorPolicy,
            mutation: createBehaviorPolicy.mutateAsync,
          }
          handleMutation(
            // @ts-ignore TODO[NVL] Weird! Check the type mismatch
            policy.mutation(policy.payload),
            DataHubNodeType.BEHAVIOR_POLICY
          )
        }
      })
      .catch((e) => {
        return toast({
          ...dataHubToastOption,
          title: t('error.publish.error', { source: DataHubNodeType.DATA_POLICY }),
          description: e.toString(),
          status: 'error',
        })
      })
      .then(() => {
        reset()
        setNode(selectedNode)
        navigate(`/datahub/${selectedNode?.type}/${selectedNode?.data.id}`, { replace: true })
      })
  }

  return (
    <Button
      data-testid="toolbox-policy-publish"
      variant="primary"
      leftIcon={<Icon as={MdPublishedWithChanges} boxSize="24px" />}
      onClick={handlePublish}
      isDisabled={!isValid || !isPolicyEditable}
      isLoading={createSchema.isPending}
    >
      {t('workspace.toolbar.policy.publish')}
    </Button>
  )
}
