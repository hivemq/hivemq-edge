import { FC, useCallback, useMemo } from 'react'
import { Node } from 'reactflow'
import { IChangeEvent } from '@rjsf/core'

import { PanelProps, SchemaData } from '../../types.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { MOCK_SCHEMA_SCHEMA } from '../../api/specs/SchemaData.ts'
import { ReactFlowSchemaForm, datahubRJSFWidgets } from '../helpers'

export const SchemaPanel: FC<PanelProps> = ({ selectedNode, onClose }) => {
  const { nodes, onUpdateNodes } = useDataHubDraftStore()

  const data = useMemo(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<SchemaData> | undefined
    return adapterNode ? adapterNode.data : null
  }, [selectedNode, nodes])

  const onFormSubmit = useCallback(
    (data: IChangeEvent) => {
      const { formData } = data
      onUpdateNodes(selectedNode, formData)
      onClose?.()
    },
    [selectedNode, onUpdateNodes, onClose]
  )

  return (
    <>
      <ReactFlowSchemaForm
        schema={MOCK_SCHEMA_SCHEMA.schema}
        uiSchema={MOCK_SCHEMA_SCHEMA.uiSchema}
        formData={data}
        widgets={datahubRJSFWidgets}
        onSubmit={onFormSubmit}
        onChange={() => console.log('changed')}
        onError={() => console.log('errors')}
      />
    </>
  )
}
