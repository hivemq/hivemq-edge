import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import type { Node } from '@xyflow/react'
import type { UseMutateAsyncFunction } from '@tanstack/react-query'
import type { UseToastOptions } from '@chakra-ui/react'
import { Button, Icon, useToast } from '@chakra-ui/react'
import { MdPublishedWithChanges } from 'react-icons/md'

import type { BehaviorPolicy, DataPolicy, PolicySchema, Script } from '@/api/__generated__'

import { useCreateDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/useCreateDataPolicy.ts'
import { useCreateBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useCreateBehaviorPolicy.ts'
import { useCreateSchema } from '@datahub/api/hooks/DataHubSchemasService/useCreateSchema.ts'
import { useCreateScript } from '@datahub/api/hooks/DataHubScriptsService/useCreateScript.ts'
import { useUpdateDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/useUpdateDataPolicy.ts'
import { useUpdateBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useUpdateBehaviorPolicy.ts'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

import { dataHubToastOption } from '@datahub/utils/toast.utils.ts'
import type { DryRunResults, ResourceState, SchemaData } from '@datahub/types.ts'
import { DataHubNodeType, ResourceWorkingVersion, DesignerStatus } from '@datahub/types.ts'

// Should be PolicySchema | Script | DataPolicy | BehaviorPolicy
// eslint-disable-next-line @typescript-eslint/no-explicit-any
interface Mutate<T, U = any> {
  type: DataHubNodeType
  payload: U
  mutation: UseMutateAsyncFunction<T, unknown, U>
}

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
  const { status, nodes, onUpdateNodes } = useDataHubDraftStore()
  const { report, node: selectedNode, setNode, reset } = usePolicyChecksStore()
  const createSchema = useCreateSchema()
  const createScript = useCreateScript()
  const createDataPolicy = useCreateDataPolicy()
  const modifyDataPolicy = useUpdateDataPolicy()
  const createBehaviorPolicy = useCreateBehaviorPolicy()
  const modifyBehaviorPolicy = useUpdateBehaviorPolicy()
  const toast = useToast()
  const navigate = useNavigate()
  const { isPolicyEditable } = usePolicyGuards()

  const isValid = !!report && report.length >= 1 && report?.every((e) => !e.error)

  // TODO[NVL] The routine doesn't change title/description based on the number of resources published
  const manageToast = (id: string, conf: UseToastOptions) => {
    const { id: _, ...cleanConfig } = conf
    if (!toast.isActive(id)) toast({ ...cleanConfig, id })
    else toast.update(id, cleanConfig)
  }

  const toastInternalError = (message: string) => {
    manageToast(`publish-internal-${selectedNode?.type}`, {
      ...dataHubToastOption,
      title: t('publish.internal.title', { source: selectedNode?.type }),
      description: message,
      status: 'error',
    })
  }

  const reportMutation = (promise: Promise<unknown>, type?: string) => {
    promise
      .then(() => {
        manageToast(`publish-success-${type || selectedNode?.type}`, {
          ...dataHubToastOption,
          title: t('publish.success.title', { source: type || selectedNode?.type }),
          description: t('publish.success.description', { source: type || selectedNode?.type, context: status }),
          status: 'success',
        })
      })
      .catch(() => {})
    return promise
  }

  /**
   * This routine is called on successful publish of a resource node (script or schema), in order to update a draft node to its published version.
   * In case the main policy fails, the graph will be holding the newly published resource nodes, but the main policy will not be updated.
   * TODO[NVL] This is so much a hack, but we need to update the draft resource nodes and we lost the link between nodes and mutation requests
   *  The whole publish process needs to be refactored
   */
  const updateDraftResourceNodes = (request: Mutate<PolicySchema> | Mutate<Script>) => (value: unknown) => {
    if (request.type === DataHubNodeType.SCHEMA) {
      const draftSchemaNodes = nodes.filter(
        (node): node is Node<SchemaData> =>
          node.type === DataHubNodeType.SCHEMA &&
          node.data.name === request.payload.id &&
          // if this is a draft, versions MUST be 1 AND ResourceWorkingVersion.DRAFT
          node.data.version === ResourceWorkingVersion.DRAFT &&
          (value as PolicySchema).version === 1
      )

      draftSchemaNodes.forEach((node) => {
        onUpdateNodes<SchemaData>(node.id, {
          ...node.data,
          version: 1,
        })
      })
    }
  }

  const publishResources = (resources?: DryRunResults<never>[]) => {
    const allSchemas =
      resources
        ?.reduce(resourceReducer<PolicySchema>(DataHubNodeType.SCHEMA), [])
        .map<Mutate<PolicySchema>>((policySchema) => ({
          type: DataHubNodeType.SCHEMA,
          payload: policySchema,
          mutation: createSchema.mutateAsync,
        })) || []

    const allScripts =
      resources?.reduce(resourceReducer<Script>(DataHubNodeType.FUNCTION), []).map<Mutate<Script>>((script) => ({
        type: DataHubNodeType.FUNCTION,
        payload: script,
        mutation: createScript.mutateAsync,
      })) || []

    return [...allSchemas, ...allScripts].map((request) =>
      reportMutation(request.mutation(request.payload), request.type).then(updateDraftResourceNodes(request))
    )
  }

  const publishMainPolicy = (payload: DryRunResults<unknown, never>) => () => {
    const { data } = payload

    let policy: Mutate<DataPolicy | BehaviorPolicy>
    switch (true) {
      case payload.node.type === DataHubNodeType.DATA_POLICY && status === DesignerStatus.DRAFT:
        policy = {
          type: DataHubNodeType.DATA_POLICY,
          payload: data as DataPolicy,
          mutation: createDataPolicy.mutateAsync,
        }
        break
      case payload.node.type === DataHubNodeType.DATA_POLICY && status === DesignerStatus.MODIFIED: {
        const dataPolicy = data as DataPolicy
        policy = {
          type: DataHubNodeType.DATA_POLICY,
          payload: {
            policyId: dataPolicy.id,
            requestBody: dataPolicy,
          },
          mutation: modifyDataPolicy.mutateAsync,
        }
        break
      }
      case payload.node.type === DataHubNodeType.BEHAVIOR_POLICY && status === DesignerStatus.DRAFT:
        policy = {
          type: DataHubNodeType.BEHAVIOR_POLICY,
          payload: data as BehaviorPolicy,
          mutation: createBehaviorPolicy.mutateAsync,
        }
        break
      case payload.node.type === DataHubNodeType.BEHAVIOR_POLICY && status === DesignerStatus.MODIFIED: {
        const behaviourPolicy = data as BehaviorPolicy
        policy = {
          type: DataHubNodeType.BEHAVIOR_POLICY,
          payload: {
            policyId: behaviourPolicy.id,
            requestBody: behaviourPolicy,
          },
          mutation: modifyBehaviorPolicy.mutateAsync,
        }
        break
      }
      default:
        toastInternalError(t('error.validityReport.noContext'))
        return
    }

    return reportMutation(policy.mutation(policy.payload), payload.node.type)
  }

  const handlePublish = () => {
    if (!report) return toastInternalError(t('error.validityReport.motFound'))

    const payload = [...report].pop()
    if (!payload) return toastInternalError(t('error.validityReport.notValid'))

    const { resources } = payload

    toast.closeAll()
    const promises = publishResources(resources)

    Promise.all(promises)
      .then(publishMainPolicy(payload))
      .then(() => {
        reset()
        setNode(selectedNode)
        navigate(`/datahub/${selectedNode?.type}/${selectedNode?.data.id}`, { replace: true })
      })
      .catch((error) => {
        let message
        if (error instanceof Error) message = error.message
        else message = String(error)

        manageToast(`publish-error-${selectedNode?.type}`, {
          ...dataHubToastOption,
          title: t('publish.error.title', { source: selectedNode?.type }),
          description: message.toString(),
          status: 'error',
        })
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
