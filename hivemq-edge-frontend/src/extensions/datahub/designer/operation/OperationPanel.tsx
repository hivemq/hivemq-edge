import { type FC, useCallback, useMemo } from 'react'
import { type Node, getIncomers } from '@xyflow/react'
import type { IChangeEvent } from '@rjsf/core'
import type { CustomValidator } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'
import { Card, CardBody } from '@chakra-ui/react'

import ErrorMessage from '@/components/ErrorMessage.tsx'

import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { getOperationSchema } from '@datahub/designer/operation/OperationPanel.utils.ts'
import { useGetFilteredFunction } from '@datahub/hooks/useGetFilteredFunctions.tsx'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import type { DataPolicyData, PanelProps } from '@datahub/types.ts'
import { DataHubNodeType, OperationData } from '@datahub/types.ts'
import { getAllParents, isFunctionNodeType, reduceIdsFrom } from '@datahub/utils/node.utils.ts'

export const OperationPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { t } = useTranslation('datahub')
  const { nodes, edges } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)
  const { data: functions } = useGetFilteredFunction()

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

  const customValidate: CustomValidator<DataPolicyData> = (formData, errors) => {
    const isIdNotUnique = Boolean(pipelineIds?.find((id) => id === formData?.id))
    if (isIdNotUnique) errors['id']?.addError(t('error.validation.operation.notUnique'))
    return errors
  }

  return (
    <Card>
      {guardAlert && <ErrorMessage status="info" type={guardAlert.title} message={guardAlert.description} />}
      <CardBody>
        <ReactFlowSchemaForm
          isNodeEditable={isNodeEditable}
          schema={schema}
          uiSchema={uiSchema}
          formData={formData}
          widgets={datahubRJSFWidgets}
          noHtml5Validate={true}
          onSubmit={onFixFormSubmit}
          customValidate={customValidate}
        />
      </CardBody>
    </Card>
  )
}
