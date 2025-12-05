import type { FC } from 'react'
import { useCallback, useEffect, useState } from 'react'
import type { Node } from '@xyflow/react'
import type { CustomValidator, UiSchema } from '@rjsf/utils'
import type { IChangeEvent } from '@rjsf/core'
import { Card, CardBody } from '@chakra-ui/react'

import type { PolicySchema } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { enumFromStringValue } from '@/utils/types.utils.ts'

import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'
import { getSchemaFamilies, getSourceFromSchema } from '@datahub/designer/schema/SchemaNode.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import { ResourceStatus, SchemaType } from '@datahub/types.ts'
import type { PanelProps, SchemaData } from '@datahub/types.ts'
import { getResourceInternalStatus } from '@datahub/utils/policy.utils.ts'

/**
 * Simplified SchemaPanel - Only allows SELECTING existing schemas
 * No creation/editing (done via SchemaEditor in main listings)
 */
export const SchemaPanelSimplified: FC<PanelProps> = ({ selectedNode, onFormSubmit, onFormError }) => {
  const { data: allSchemas, isLoading, isSuccess, error } = useGetAllSchemas()
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)
  const [formData, setFormData] = useState<SchemaData | null>(null)

  useEffect(() => {
    if (!allSchemas) return
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<SchemaData> | undefined
    if (!adapterNode) return

    // Find the specific schema version that matches the node's data
    const nodeVersion = adapterNode.data.version
    const schema = allSchemas.items?.find((s) => s.id === adapterNode.data.name && s.version === nodeVersion)

    if (schema) {
      const families = getSchemaFamilies(allSchemas.items || [])
      const schemaType = enumFromStringValue(SchemaType, schema.type) || SchemaType.JSON
      const schemaSource = getSourceFromSchema(schema)

      setFormData({
        name: schema.id,
        type: schemaType,
        version: schema.version || 1,
        schemaSource,
        messageType: schema.arguments?.messageType, // Load messageType from schema arguments
        internalVersions: families[schema.id].versions,
        internalStatus: ResourceStatus.LOADED,
      })
    } else {
      // Fallback: schema not found, use node data as-is
      const internalState = getResourceInternalStatus<PolicySchema>(
        adapterNode.data.name,
        allSchemas,
        getSchemaFamilies
      )
      const intData: SchemaData = { ...adapterNode.data, ...internalState }
      setFormData(intData)
    }
  }, [allSchemas, nodes, selectedNode])

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
        const schema = allSchemas?.items?.findLast((schema) => schema.id === newData.name)
        if (schema) {
          const schemaType = enumFromStringValue(SchemaType, schema.type) || SchemaType.JSON
          const schemaSource = getSourceFromSchema(schema)

          setFormData({
            name: schema.id,
            type: schemaType,
            version: schema.version || 1,
            schemaSource,
            messageType: schema.arguments?.messageType, // Load messageType from schema arguments
            internalVersions: getSchemaFamilies(allSchemas?.items || [])[schema.id].versions,
            internalStatus: ResourceStatus.LOADED,
          })
        }
        return
      }

      // Handle version change
      if (id?.includes('version') && formData) {
        const schema = allSchemas?.items?.find(
          (schema) => schema.id === formData.name && schema.version?.toString() === newData.version.toString()
        )
        if (schema) {
          const schemaSource = getSourceFromSchema(schema)

          setFormData({
            ...formData,
            version: newData.version,
            schemaSource,
            messageType: schema.arguments?.messageType, // Load messageType from schema arguments
            internalStatus: ResourceStatus.LOADED,
          })
        }
        return
      }

      setFormData(newData)
    },
    [allSchemas?.items, formData]
  )

  const customValidate: CustomValidator<SchemaData> = useCallback((formData, errors) => {
    if (!formData) return errors
    return errors
  }, [])

  const getUISchema = (schema: SchemaData | null): UiSchema => {
    const { internalVersions } = schema || {}
    return {
      'ui:order': ['name', 'version', 'type', 'schemaSource', 'messageType', '*'],
      name: {
        'ui:widget': 'datahub:schema-name-select',
      },
      version: {
        'ui:widget': 'datahub:version',
        'ui:options': {
          selectOptions: internalVersions,
        },
      },
      type: {
        'ui:readonly': true,
      },
      schemaSource: {
        'ui:widget': schema?.type === SchemaType.PROTOBUF ? 'application/octet-stream' : 'application/schema+json',
        'ui:readonly': true,
        'ui:options': {
          readOnly: true,
        },
      },
      messageType: {
        'ui:widget': 'datahub:message-type', // Custom widget for protobuf message type selection
        'ui:readonly': true, // Readonly in simplified panel (can only be set during creation)
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
            schema={MOCK_SCHEMA_SCHEMA.schema}
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
