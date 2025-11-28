import type { FC } from 'react'
import { useCallback, useEffect, useState } from 'react'
import type { Node } from '@xyflow/react'
import { Card, CardBody } from '@chakra-ui/react'
import type { CustomValidator, UiSchema } from '@rjsf/utils'
import type { IChangeEvent } from '@rjsf/core'

import type { Script } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'

import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_FUNCTION_SCHEMA } from '@datahub/designer/script/FunctionData.ts'
import { getScriptFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import type { FunctionData, PanelProps } from '@datahub/types.ts'
import { ResourceStatus } from '@datahub/types.ts'
import { getResourceInternalStatus } from '@datahub/utils/policy.utils.ts'

/**
 * Simplified FunctionPanel - Only allows SELECTING existing scripts
 * No creation/editing (done via ScriptEditor in main listings)
 */
export const FunctionPanelSimplified: FC<PanelProps> = ({ selectedNode, onFormSubmit, onFormError }) => {
  const { data: allScripts, isLoading, isSuccess, error } = useGetAllScripts({})
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)
  const [formData, setFormData] = useState<FunctionData | null>(null)

  useEffect(() => {
    if (!allScripts) return
    const sourceNode = nodes.find((node) => node.id === selectedNode) as Node<FunctionData> | undefined
    if (!sourceNode) return

    // Find the specific script version that matches the node's data
    const nodeVersion = sourceNode.data.version
    const script = allScripts.items?.find((s) => s.id === sourceNode.data.name && s.version === nodeVersion)

    if (script) {
      const families = getScriptFamilies(allScripts.items || [])
      setFormData({
        name: script.id,
        type: 'Javascript',
        version: script.version || 1,
        description: script.description,
        sourceCode: atob(script.source),
        internalVersions: families[script.id].versions,
        internalStatus: ResourceStatus.LOADED,
      })
    } else {
      // Fallback: script not found, use node data as-is
      const internalState = getResourceInternalStatus<Script>(sourceNode.data.name, allScripts, getScriptFamilies)
      const intData: FunctionData = { ...sourceNode.data, ...internalState }
      setFormData(intData)
    }
  }, [allScripts, nodes, selectedNode])

  useEffect(() => {
    if (error) onFormError?.(error)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [error])

  const onReactFlowSchemaFormChange = useCallback(
    (changeEvent: IChangeEvent, id?: string | undefined) => {
      const newData = changeEvent.formData
      if (!newData) return

      // Handle name change
      if (id?.includes('name')) {
        const selectedScript = allScripts?.items?.findLast((script) => script.id === newData.name)
        if (selectedScript) {
          setFormData({
            name: selectedScript.id,
            type: 'Javascript',
            version: selectedScript.version || 1,
            description: selectedScript.description,
            sourceCode: atob(selectedScript.source),
            internalVersions: getScriptFamilies(allScripts?.items || [])[selectedScript.id].versions,
            internalStatus: ResourceStatus.LOADED,
          })
        }
        return
      }

      // Handle version change
      if (id?.includes('version') && formData) {
        const versionedScript = allScripts?.items?.find(
          (script) => script.id === formData.name && script.version?.toString() === newData.version.toString()
        )
        if (versionedScript) {
          setFormData({
            ...formData,
            version: newData.version,
            description: versionedScript.description,
            sourceCode: atob(versionedScript.source),
            internalStatus: ResourceStatus.LOADED,
          })
        }
        return
      }

      setFormData(newData)
    },
    [allScripts?.items, formData]
  )

  const customValidate: CustomValidator<FunctionData> = useCallback((formData, errors) => {
    if (!formData) return errors
    if (!formData.name) errors.name?.addError('Please select a script')
    if (!formData.version) errors.version?.addError('Please select a version')
    return errors
  }, [])

  const getUISchema = (script: FunctionData | null): UiSchema => {
    const { internalVersions } = script || {}
    return {
      'ui:order': ['name', 'version', 'description', 'sourceCode', '*'],
      type: {
        'ui:widget': 'hidden',
      },
      name: {
        'ui:widget': 'datahub:function-name-select',
      },
      version: {
        'ui:widget': 'datahub:version',
        'ui:options': {
          selectOptions: internalVersions,
        },
      },
      sourceCode: {
        'ui:widget': 'text/javascript',
        'ui:readonly': true,
        'ui:options': {
          readOnly: true,
        },
      },
      description: {
        'ui:readonly': true,
      },
    }
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
            schema={MOCK_FUNCTION_SCHEMA.schema}
            uiSchema={getUISchema(formData)}
            formData={formData}
            widgets={datahubRJSFWidgets}
            customValidate={customValidate}
            onSubmit={onFormSubmit}
            onChange={onReactFlowSchemaFormChange}
          />
        </CardBody>
      )}
    </Card>
  )
}
