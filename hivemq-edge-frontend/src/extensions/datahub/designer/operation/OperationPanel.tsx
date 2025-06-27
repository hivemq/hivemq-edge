import { type FC, useCallback, useEffect, useMemo } from 'react'
import { type Node, getIncomers } from '@xyflow/react'
import type { IChangeEvent } from '@rjsf/core'
import type { CustomValidator } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'
import { Card, CardBody } from '@chakra-ui/react'

import type { BehaviorPolicyTransitionEvent } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { getOperationSchema } from '@datahub/designer/operation/OperationPanel.utils.ts'
import { useFilteredFunctionsFetcher } from '@datahub/hooks/useFilteredFunctionsFetcher.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import type { DataPolicyData, PanelProps, TransitionData } from '@datahub/types.ts'
import { DataHubNodeType, OperationData } from '@datahub/types.ts'
import { getAllParents, isFunctionNodeType, reduceIdsFrom } from '@datahub/utils/node.utils.ts'

interface OperationPanelContext {
  type: DataHubNodeType | undefined
  transition: BehaviorPolicyTransitionEvent | undefined
}

export const OperationPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit, onFormError }) => {
  const { t } = useTranslation('datahub')
  const { nodes, edges } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const context = useMemo<OperationPanelContext>(() => {
    const operationNode = nodes.find((e) => e.id === selectedNode) as Node<OperationData> | undefined
    if (!operationNode?.data) return { type: undefined, transition: undefined }

    const set = getAllParents(operationNode, nodes, edges)
    const type =
      (Array.from(set).find((e) => e.type === DataHubNodeType.DATA_POLICY || e.type === DataHubNodeType.BEHAVIOR_POLICY)
        ?.type as DataHubNodeType) ?? DataHubNodeType.DATA_POLICY

    const transition = (
      Array.from(set).find((e) => e.type === DataHubNodeType.TRANSITION)?.data as TransitionData | undefined
    )?.event

    return { type, transition }
  }, [edges, nodes, selectedNode])

  const { getFilteredFunctions, isLoading, isSuccess, error } = useFilteredFunctionsFetcher()
  const functions = getFilteredFunctions(context.type, context.transition)

  const formData = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<OperationData> | undefined
    if (!adapterNode?.data) return null

    if (adapterNode.data.functionId === OperationData.Function.DATAHUB_TRANSFORM) {
      const connectedFunctionIds = getIncomers(adapterNode, nodes, edges)
        .filter(isFunctionNodeType)
        .map((e) => e.data.name)
      const operationPayload = adapterNode.data.formData as unknown as OperationData.DataHubTransformType

      const cleanedUp = operationPayload.transform.filter((id) => connectedFunctionIds.includes(id))
      const notIncluded = connectedFunctionIds.filter((id) => !cleanedUp.includes(id))
      operationPayload.transform = [...cleanedUp, ...notIncluded]
    }

    return adapterNode?.data
  }, [edges, nodes, selectedNode])

  const pipelineIds = useMemo(() => {
    const operationNode = nodes.find((e) => e.id === selectedNode) as Node<OperationData> | undefined
    if (!operationNode?.data) return null

    const set = getAllParents(operationNode, nodes, edges)
    return Array.from(set).reduce<string[]>(reduceIdsFrom<OperationData>(DataHubNodeType.OPERATION, selectedNode), [])
  }, [edges, nodes, selectedNode])

  const onFixFormSubmit = useCallback(
    (initData: IChangeEvent<OperationData>) => {
      const { formData } = initData
      if (formData) {
        const { id, functionId } = formData
        const functionSpecs = functions.find((e) => e.functionId === functionId)
        if (functionSpecs) {
          const { metadata } = functionSpecs
          initData.formData = { id, ...initData.formData, metadata }
        }
      }
      onFormSubmit?.(initData)
    },
    [functions, onFormSubmit]
  )

  const { schema, uiSchema } = useMemo(() => {
    return getOperationSchema(functions)
  }, [functions])

  useEffect(() => {
    if (error) onFormError?.(error)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [error])

  const customValidate: CustomValidator<DataPolicyData> = (formData, errors) => {
    const isIdNotUnique = Boolean(pipelineIds?.find((id) => id === formData?.id))
    if (isIdNotUnique) errors['id']?.addError(t('error.validation.operation.notUnique'))
    return errors
  }

  return (
    <Card>
      {isLoading && <LoaderSpinner />}
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      {error && <ErrorMessage status="error" message={error.message} />}
      {isSuccess && (
        <CardBody>
          <ReactFlowSchemaForm
            isNodeEditable={isNodeEditable}
            schema={schema}
            uiSchema={uiSchema}
            formData={formData}
            formContext={{ functions }}
            widgets={datahubRJSFWidgets}
            noHtml5Validate={true}
            onSubmit={onFixFormSubmit}
            customValidate={customValidate}
          />
        </CardBody>
      )}
    </Card>
  )
}
