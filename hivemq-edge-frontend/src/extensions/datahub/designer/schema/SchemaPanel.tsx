import type { FC } from 'react'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { Node } from '@xyflow/react'
import type { CustomValidator, UiSchema } from '@rjsf/utils'
import type { IChangeEvent } from '@rjsf/core'
import { Card, CardBody } from '@chakra-ui/react'
import { parse } from 'protobufjs'

import type { PolicySchema } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { enumFromStringValue } from '@/utils/types.utils.ts'

import { MOCK_JSONSCHEMA_SCHEMA, MOCK_PROTOBUF_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.ts'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'
import { getSchemaFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'
import { ResourceStatus, ResourceWorkingVersion, SchemaType } from '@datahub/types.ts'
import type { PanelProps, SchemaData } from '@datahub/types.ts'
import { getResourceInternalStatus } from '@datahub/utils/policy.utils.ts'

export const SchemaPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit, onFormError }) => {
  const { data: allSchemas, isLoading, isSuccess, error } = useGetAllSchemas()
  const { nodes } = useDataHubDraftStore()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)
  const [formData, setFormData] = useState<SchemaData | null>(null)
  const isProgrammaticUpdateRef = useRef(false)

  useEffect(() => {
    if (!allSchemas) return
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<SchemaData> | undefined
    if (!adapterNode) return

    const internalState = getResourceInternalStatus<PolicySchema>(adapterNode.data.name, allSchemas, getSchemaFamilies)
    const intData: SchemaData = { ...adapterNode.data, ...internalState }
    setFormData(intData)
  }, [allSchemas, nodes, selectedNode])

  useEffect(() => {
    if (error) onFormError?.(error)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [error])

  const onReactFlowSchemaFormChange = useCallback(
    (changeEvent: IChangeEvent, id?: string | undefined) => {
      // Ignore onChange events triggered by programmatic updates
      if (isProgrammaticUpdateRef.current) {
        isProgrammaticUpdateRef.current = false
        return
      }

      // TODO[NVL] id have form "root_XXXXXX", so conditions are not particularly type-safe as expected

      // Handle name change - load schema/type or create a new draft
      if (id?.includes('name')) {
        const schema = allSchemas?.items?.findLast((schema) => schema.id === changeEvent.formData.name)
        if (schema) {
          isProgrammaticUpdateRef.current = true
          setFormData({
            name: schema.id,
            type: enumFromStringValue(SchemaType, schema.type) || SchemaType.JSON,
            version: schema.version || ResourceWorkingVersion.MODIFIED,
            schemaSource: atob(schema.schemaDefinition),
            internalVersions: getSchemaFamilies(allSchemas?.items || [])[schema.id].versions,
            internalStatus: ResourceStatus.LOADED,
          })
          // Reset flag after React processes the state update
          queueMicrotask(() => {
            isProgrammaticUpdateRef.current = false
          })
        } else {
          isProgrammaticUpdateRef.current = true
          setFormData({
            internalStatus: ResourceStatus.DRAFT,
            name: changeEvent.formData.name,
            type: SchemaType.JSON,
            version: ResourceWorkingVersion.DRAFT,
            schemaSource: MOCK_JSONSCHEMA_SCHEMA,
          })
          // Reset flag after React processes the state update
          queueMicrotask(() => {
            isProgrammaticUpdateRef.current = false
          })
        }
        return
      }

      // Handle type change - update schema source based on type
      if (id?.includes('type') && formData) {
        if (formData.type !== changeEvent.formData.type) {
          isProgrammaticUpdateRef.current = true
          setFormData({
            ...formData,
            type: changeEvent.formData.type,
            schemaSource: changeEvent.formData.type === SchemaType.JSON ? MOCK_JSONSCHEMA_SCHEMA : MOCK_PROTOBUF_SCHEMA,
            version:
              formData.version === ResourceWorkingVersion.DRAFT
                ? ResourceWorkingVersion.DRAFT
                : ResourceWorkingVersion.MODIFIED,
            internalStatus:
              formData.internalStatus === ResourceStatus.DRAFT ? ResourceStatus.DRAFT : ResourceStatus.MODIFIED,
          })
          // Reset flag after React processes the state update
          queueMicrotask(() => {
            isProgrammaticUpdateRef.current = false
          })
        }
        return
      }

      // Handle version change - load specific version
      if (id?.includes('version') && formData) {
        const schema = allSchemas?.items?.find(
          (schema) =>
            schema.id === formData.name && schema.version?.toString() === changeEvent.formData.version.toString()
        )

        if (schema) {
          isProgrammaticUpdateRef.current = true
          setFormData({
            ...formData,
            version: changeEvent.formData.version,
            schemaSource: atob(schema.schemaDefinition),
            internalStatus: ResourceStatus.LOADED,
          })
          // Reset flag after React processes the state update
          queueMicrotask(() => {
            isProgrammaticUpdateRef.current = false
          })
        }
        return
      }

      // Handle schemaSource change - mark as modified
      if (id?.includes('schemaSource') && formData) {
        if (formData.internalStatus === ResourceStatus.LOADED || formData.internalStatus === ResourceStatus.DRAFT) {
          isProgrammaticUpdateRef.current = true
          setFormData({
            ...formData,
            schemaSource: changeEvent.formData.schemaSource,
            version:
              formData.version === ResourceWorkingVersion.DRAFT
                ? ResourceWorkingVersion.DRAFT
                : ResourceWorkingVersion.MODIFIED,
            internalStatus:
              formData.internalStatus === ResourceStatus.DRAFT ? ResourceStatus.DRAFT : ResourceStatus.MODIFIED,
          })
          // Reset flag after React processes the state update
          queueMicrotask(() => {
            isProgrammaticUpdateRef.current = false
          })
        }
        return
      }
    },
    [allSchemas?.items, formData]
  )

  const customValidate: CustomValidator<SchemaData> = (formData, errors) => {
    if (!formData) return errors
    const { type, schemaSource } = formData

    // if (type === SchemaType.JAVASCRIPT && schemaSource) {
    //   try {
    //     const program = Parser.parse(schemaSource, { ecmaVersion: 'latest' })
    //   } catch (e) {
    //     errors.schemaSource?.addError((e as SyntaxError).message)
    //   }
    // }
    // if (type === SchemaType.JSON && schemaSource) {
    //   try {
    //     const validator = customizeValidator()
    //     const parsed: RJSFSchema = JSON.parse(schemaSource)
    //     const validated = validator.validateFormData(undefined, { ...jj, required: [] })
    //   } catch (e) {
    //     errors.schemaSource?.addError((e as SyntaxError).message)
    //     // setFields(null)
    //   }
    // }

    if (type === SchemaType.PROTOBUF && schemaSource) {
      try {
        parse(schemaSource)
      } catch (e) {
        errors.schemaSource?.addError((e as SyntaxError).message)
      }
    }

    return errors
  }

  const getUISchema = (schema: SchemaData | null): UiSchema => {
    const { internalVersions } = schema || {}
    return {
      name: {
        'ui:widget': 'datahub:schema-name',
        'ui:options': {
          isDraft: schema?.version === ResourceWorkingVersion.DRAFT,
        },
      },
      version: {
        'ui:widget': 'datahub:version',
        'ui:options': {
          selectOptions: internalVersions,
        },
      },
      schemaSource: {
        'ui:widget': schema?.type === SchemaType.PROTOBUF ? 'application/octet-stream' : 'application/schema+json',
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
