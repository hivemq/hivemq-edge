import { FC, useCallback, useMemo } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'
import { IChangeEvent } from '@rjsf/core'

import { MOCK_OPERATION_SCHEMA } from '../../api/specs/'
import { OperationData, PanelProps } from '../../types.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { ReactFlowSchemaForm, datahubRJSFWidgets } from '../helpers'

export const OperationPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { nodes, functions } = useDataHubDraftStore()

  const formData = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<OperationData> | undefined
    if (!adapterNode?.data) return null
    return adapterNode?.data
  }, [nodes, selectedNode])

  const onFixFormSubmit = useCallback(
    (data: IChangeEvent<OperationData>) => {
      const initData = data
      const { formData } = initData
      if (formData) {
        const { functionId } = formData
        const fct = functions.find((e) => e.functionId === functionId)
        if (fct) {
          const { metadata } = fct
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
