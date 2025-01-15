import { FC, useCallback, useState } from 'react'
import { Node } from 'reactflow'
import { Card, CardBody } from '@chakra-ui/react'
import { UiSchema } from '@rjsf/utils'
import { IChangeEvent } from '@rjsf/core/src/components/Form.tsx'

import { MOCK_JAVASCRIPT_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import { FunctionData, PanelProps, ResourceStatus, ResourceWorkingVersion } from '@datahub/types.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.tsx'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_FUNCTION_SCHEMA } from '@datahub/designer/script/FunctionData.ts'
import { getScriptFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.tsx'

export const FunctionPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { data: allScripts } = useGetAllScripts({})
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)

  const [formData, setFormData] = useState<FunctionData | null>(() => {
    const sourceNode = nodes.find((node) => node.id === selectedNode) as Node<FunctionData> | undefined

    const internalStatus =
      typeof sourceNode?.data.version === 'number' ? ResourceStatus.LOADED : sourceNode?.data.version

    return sourceNode ? { ...sourceNode.data, internalStatus } : null
  })

  const getUISchema = (script: FunctionData | null): UiSchema => {
    const { internalStatus, internalVersions } = script || {}
    return {
      type: {
        'ui:widget': 'hidden',
      },
      // 'ui:order':
      //   internalStatus === ResourceStatus.DRAFT || !internalStatus
      //     ? ['name', 'type', 'schemaSource', 'messageType', 'version']
      //     : ['name', 'version', 'schemaSource', 'messageType', 'type'],
      name: {
        'ui:widget': 'datahub:function-name',
        'ui:options': {
          isDraft: script?.version === ResourceWorkingVersion.DRAFT,
        },
      },
      version: {
        'ui:widget': 'datahub:version',
        'ui:options': {
          readonly:
            internalStatus === ResourceStatus.DRAFT || internalStatus === ResourceStatus.MODIFIED || !internalStatus,
          selectOptions: internalVersions,
        },
      },
      sourceCode: {
        'ui:widget': 'text/javascript',
        'ui:options': {
          // readonly: !internalStatus,
        },
      },
      description: {
        'ui:placeholder': 'A short description for this version',
      },
    }
  }

  const onReactFlowSchemaFormChange = useCallback(
    (changeEvent: IChangeEvent, id?: string | undefined) => {
      if (id?.includes('name')) {
        const selectedScript = allScripts?.items?.findLast((script) => script.id === changeEvent.formData.name)
        if (selectedScript) {
          setFormData({
            name: selectedScript.id,
            type: 'Javascript',
            version: selectedScript.version || ResourceWorkingVersion.MODIFIED,
            description: changeEvent.formData.description,
            sourceCode: atob(selectedScript.source),
            internalVersions: getScriptFamilies(allScripts?.items || [])[selectedScript.id].versions,
            internalStatus: ResourceStatus.LOADED,
          })
        } else {
          setFormData({
            name: changeEvent.formData.name,
            type: 'Javascript',
            version: ResourceWorkingVersion.DRAFT,
            sourceCode: MOCK_JAVASCRIPT_SCHEMA,
          })
        }
        return
      }
      if (
        (id?.includes('sourceCode') || id?.includes('description')) &&
        formData &&
        formData.internalStatus === ResourceStatus.LOADED
      ) {
        setFormData({
          ...formData,
          version: ResourceWorkingVersion.MODIFIED,
          internalStatus: ResourceStatus.MODIFIED,
        })
        return
      }
      if (id?.includes('version') && formData) {
        const versionedScript = allScripts?.items?.find(
          (script) => script.id === formData.name && script.version?.toString() === changeEvent.formData.version
        )
        if (versionedScript) {
          setFormData({
            ...formData,
            version: changeEvent.formData.version,
            description: versionedScript.description,
            sourceCode: atob(versionedScript.source),
          })
        }
        return
      }
    },
    [allScripts?.items, formData]
  )

  return (
    <Card>
      {guardAlert && guardAlert}
      <CardBody>
        <ReactFlowSchemaForm
          isNodeEditable={isNodeEditable}
          widgets={datahubRJSFWidgets}
          schema={MOCK_FUNCTION_SCHEMA.schema}
          uiSchema={getUISchema(formData)}
          formData={formData}
          onSubmit={onFormSubmit}
          onChange={onReactFlowSchemaFormChange}
        />
      </CardBody>
    </Card>
  )
}
