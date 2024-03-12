import { FC, useCallback, useMemo } from 'react'
import { getIncomers, Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'
import { IChangeEvent } from '@rjsf/core'

import { OperationData, PanelProps } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_OPERATION_SCHEMA } from '@datahub/designer/operation/OperationData.ts'
import { isFunctionNodeType } from '@datahub/utils/node.utils.ts'

export const OperationPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes, edges, functions } = useDataHubDraftStore()

  const formData = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<OperationData> | undefined
    if (!adapterNode?.data) return null

    if (adapterNode.data.functionId === OperationData.Function.DATAHUB_TRANSFORM) {
      const connectedFunctionIds = getIncomers(adapterNode, nodes, edges)
        .filter(isFunctionNodeType)
        .map((e) => e.data.name)
      const gg = adapterNode.data.formData as unknown as OperationData.DataHubTransformType

      const cleanedUp = gg.transform.filter((id) => connectedFunctionIds.includes(id))
      const notIncluded = connectedFunctionIds.filter((id) => !cleanedUp.includes(id))
      gg.transform = [...cleanedUp, ...notIncluded]
    }

    return adapterNode?.data
  }, [edges, nodes, selectedNode])

  const onFixFormSubmit = useCallback(
    (data: IChangeEvent<OperationData>) => {
      const initData = data
      const { formData } = initData
      if (formData) {
        const { functionId } = formData
        const functionSpecs = functions.find((e) => e.functionId === functionId)
        if (functionSpecs) {
          const { metadata } = functionSpecs
          initData.formData = { ...initData.formData, metadata }
        }
      }
      onFormSubmit?.(initData)
    },
    [functions, onFormSubmit]
  )

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
        />
      </CardBody>
    </Card>
  )
}
