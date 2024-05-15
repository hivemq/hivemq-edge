import { FC, useCallback, useMemo } from 'react'
import { getIncomers, Node } from 'reactflow'
import { IChangeEvent } from '@rjsf/core'
import { CustomValidator } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'

import { Card, CardBody } from '@chakra-ui/react'

import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_OPERATION_SCHEMA } from '@datahub/designer/operation/OperationData.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { isFunctionNodeType } from '@datahub/utils/node.utils.ts'
import { DataHubNodeType, DataPolicyData, OperationData, PanelProps } from '@datahub/types.ts'

export const OperationPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { t } = useTranslation('datahub')
  const { nodes, edges, functions } = useDataHubDraftStore()

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

    const getAllParents = (node: Node, visited = new Set<Node>()): Set<Node> => {
      if (visited.has(node)) return visited
      visited.add(node)
      for (const incomer of getIncomers(node, nodes, edges)) {
        getAllParents(incomer, visited)
      }
      return visited
    }

    const set = getAllParents(operationNode)
    return Array.from(set).reduce<string[]>((acc, node) => {
      if (node.type === DataHubNodeType.OPERATION) {
        const { id, data } = node satisfies Node<OperationData>
        if (data.id && id !== selectedNode) {
          acc.push(data.id)
        }
      }
      return acc
    }, [])
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

  const customValidate: CustomValidator<DataPolicyData> = (formData, errors) => {
    const isIdNotUnique = Boolean(pipelineIds?.find((id) => id === formData?.id))
    if (isIdNotUnique) errors['id']?.addError(t('error.validation.operation.notUnique'))
    return errors
  }

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_OPERATION_SCHEMA.schema}
          uiSchema={MOCK_OPERATION_SCHEMA.uiSchema}
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
