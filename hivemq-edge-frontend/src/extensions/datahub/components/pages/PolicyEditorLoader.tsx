import type { FC } from 'react'
import { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Spinner, useToast } from '@chakra-ui/react'
import type { Connection, NodeAddChange } from '@xyflow/react'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { DataHubNodeType, DesignerStatus, DesignerPolicyType } from '@datahub/types.ts'
import PolicyEditor from '@datahub/components/pages/PolicyEditor.tsx'
import { useGetDataPolicy } from '@datahub/api/hooks/DataHubDataPoliciesService/useGetDataPolicy.ts'
import { useGetBehaviorPolicy } from '@datahub/api/hooks/DataHubBehaviorPoliciesService/useGetBehaviorPolicy.ts'
import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.ts'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.ts'
import { loadDataPolicy } from '@datahub/designer/data_policy/DataPolicyNode.utils.ts'
import { loadTopicFilter } from '@datahub/designer/topic_filter/TopicFilterNode.utils.ts'
import { loadValidators } from '@datahub/designer/validator/ValidatorNode.utils.ts'
import { loadDataPolicyPipelines } from '@datahub/designer/operation/OperationNode.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DATAHUB_TOAST_ID, dataHubToastOption } from '@datahub/utils/toast.utils.ts'
import { loadBehaviorPolicy } from '@datahub/designer/behavior_policy/BehaviorPolicyNode.utils.ts'
import { loadClientFilter } from '@datahub/designer/client_filter/ClientFilterNode.utils.ts'
import { loadTransitions } from '@datahub/designer/transition/TransitionNode.utils.ts'

interface PolicyLoaderProps {
  policyId: string
}

export const DataPolicyLoader: FC<PolicyLoaderProps> = ({ policyId }) => {
  const { t } = useTranslation('datahub')
  const store = useDataHubDraftStore()
  const toast = useToast()

  const {
    isLoading: isDataPolicyLoading,
    data: dataPolicy,
    isError: isDataPolicyError,
    error,
  } = useGetDataPolicy(policyId)
  // TODO[19966] Waste of bandwidth; enable the request only is dataPolicy contains the relevant resources
  const { isLoading: isScriptLoading, data: scripts, isError: isScriptError, error: scriptError } = useGetAllScripts({})
  const { isLoading: isSchemaLoading, data: schemas, isError: isSchemaError, error: schemaError } = useGetAllSchemas()

  useEffect(() => {
    if (!dataPolicy || !schemas || !scripts) return

    try {
      store.reset()
      const policyNode = loadDataPolicy(dataPolicy)
      const filterNode = loadTopicFilter(dataPolicy, policyNode.item)
      const validatorNodes = loadValidators(dataPolicy, schemas.items || [], policyNode.item)
      const pipelines = loadDataPolicyPipelines(dataPolicy, schemas.items || [], scripts.items || [], policyNode.item)

      const allArtefactsLoaded = [policyNode, ...filterNode, ...validatorNodes, ...pipelines]

      const nodeChanges = allArtefactsLoaded.reduce<NodeAddChange[]>((acc, element) => {
        const allIds = acc.map((nodeAddChange) => nodeAddChange.item.id)
        const nodeChange = element as NodeAddChange
        if (nodeChange.item && !allIds.includes(nodeChange.item.id)) acc.push(nodeChange)
        return acc
      }, [])
      const edgeConnects = allArtefactsLoaded.filter(
        (element): element is Connection => !!(element as Connection).source
      )

      store.onNodesChange(nodeChanges)
      for (const connection of edgeConnects) {
        store.onConnect(connection)
      }
      store.setStatus(DesignerStatus.LOADED, { name: dataPolicy.id, type: DataHubNodeType.DATA_POLICY })
    } catch (error) {
      let message
      if (error instanceof Error) message = error.message
      else message = String(error)
      if (!toast.isActive(DATAHUB_TOAST_ID))
        toast({
          ...dataHubToastOption,
          id: DATAHUB_TOAST_ID,
          title: t('error.load.errorTitle', { source: DesignerPolicyType.DATA_POLICY }),
          description: message,
          status: 'error',
        })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dataPolicy, schemas, scripts, t, toast])

  if (isDataPolicyLoading || isScriptLoading || isSchemaLoading) return <Spinner />
  if (isDataPolicyError) return <ErrorMessage type={t('error.notDefined.title')} message={error?.message} />
  if (isScriptError) return <ErrorMessage type={t('error.notDefined.title')} message={scriptError?.message} />
  if (isSchemaError) return <ErrorMessage type={t('error.notDefined.title')} message={schemaError?.message} />

  return <PolicyEditor />
}

export const BehaviorPolicyLoader: FC<PolicyLoaderProps> = ({ policyId }) => {
  const { t } = useTranslation('datahub')
  const store = useDataHubDraftStore()
  const toast = useToast()

  const {
    isLoading: isPolicyLoading,
    data: behaviorPolicy,
    isError: isPolicyError,
    error,
  } = useGetBehaviorPolicy(policyId)
  // TODO[19966] Waste of bandwidth; enable the request only is dataPolicy contains the relevant resources
  const { isLoading: isScriptLoading, data: scripts, isError: isScriptError, error: scriptError } = useGetAllScripts({})
  const { isLoading: isSchemaLoading, data: schemas, isError: isSchemaError, error: schemaError } = useGetAllSchemas()

  useEffect(() => {
    if (!behaviorPolicy || !schemas || !scripts) return

    try {
      store.reset()
      const behaviorPolicyNode = loadBehaviorPolicy(behaviorPolicy)
      const filterNode = loadClientFilter(behaviorPolicy, behaviorPolicyNode.item)
      const pipelines = loadTransitions(
        behaviorPolicy,
        schemas.items || [],
        scripts.items || [],
        behaviorPolicyNode.item
      )

      const allArtefactsLoaded = [behaviorPolicyNode, ...filterNode, ...pipelines]
      const nodeChanges = allArtefactsLoaded.reduce<NodeAddChange[]>((acc, element) => {
        const allIds = acc.map((nodeAddChange) => nodeAddChange.item.id)
        const nodeChange = element as NodeAddChange
        if (nodeChange.item && !allIds.includes(nodeChange.item.id)) acc.push(nodeChange)
        return acc
      }, [])
      const edgeConnects = allArtefactsLoaded.filter(
        (element): element is Connection => !!(element as Connection).source
      )

      store.onNodesChange(nodeChanges)
      for (const connection of edgeConnects) {
        store.onConnect(connection)
      }
      store.setStatus(DesignerStatus.LOADED, { name: behaviorPolicy.id, type: DataHubNodeType.BEHAVIOR_POLICY })
    } catch (error) {
      let message
      if (error instanceof Error) message = error.message
      else message = String(error)
      if (!toast.isActive(DATAHUB_TOAST_ID))
        toast({
          ...dataHubToastOption,
          id: DATAHUB_TOAST_ID,
          title: t('error.load.errorTitle', { source: DesignerPolicyType.DATA_POLICY }),
          description: message,
          status: 'error',
        })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [behaviorPolicy, schemas, scripts, t, toast])

  if (isPolicyLoading || isScriptLoading || isSchemaLoading) return <Spinner />
  if (isPolicyError) return <ErrorMessage type={t('error.notDefined.title')} message={error?.message} />
  if (isScriptError) return <ErrorMessage type={t('error.notDefined.title')} message={scriptError?.message} />
  if (isSchemaError) return <ErrorMessage type={t('error.notDefined.title')} message={schemaError?.message} />
  return <PolicyEditor />
}

const PolicyEditorLoader: FC = () => {
  const { t } = useTranslation('datahub')
  const { policyType, policyId } = useParams()

  if (!policyType || !(policyType in DesignerPolicyType))
    return <ErrorMessage type={t('error.notDefined.title')} message={t('error.notDefined.description')} />

  if (policyId) {
    if (policyType === DesignerPolicyType.DATA_POLICY) return <DataPolicyLoader policyId={policyId} />
    if (policyType === DesignerPolicyType.BEHAVIOR_POLICY) return <BehaviorPolicyLoader policyId={policyId} />

    return <ErrorMessage type={t('error.notDefined.title')} message={t('error.notDefined.description')} />
  }

  return <PolicyEditor />
}

export default PolicyEditorLoader
