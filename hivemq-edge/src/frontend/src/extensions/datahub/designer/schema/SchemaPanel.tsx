import { FC, useCallback, useState } from 'react'
import { Node } from 'reactflow'
import { parse } from 'protobufjs'
import { CustomValidator, UiSchema } from '@rjsf/utils'
import { IChangeEvent } from '@rjsf/core/src/components/Form.tsx'
import { Card, CardBody } from '@chakra-ui/react'

import { enumFromStringValue } from '@/utils/types.utils.ts'

import { PanelProps, ResourceStatus, SchemaData, SchemaType } from '@datahub/types.ts'
import { MOCK_JSONSCHEMA_SCHEMA, MOCK_PROTOBUF_SCHEMA } from '@datahub/__test-utils__/schema.mocks.ts'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.tsx'
import { ReactFlowSchemaForm } from '@datahub/components/forms/ReactFlowSchemaForm.tsx'
import { datahubRJSFWidgets } from '@datahub/designer/datahubRJSFWidgets.tsx'
import { MOCK_SCHEMA_SCHEMA } from '@datahub/designer/schema/SchemaData.ts'
import { getSchemaFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

export const SchemaPanel: FC<PanelProps> = ({ selectedNode, onFormSubmit }) => {
  const { data: allSchemas } = useGetAllSchemas()
  const { nodes } = useDataHubDraftStore()
  const [formData, setFormData] = useState<SchemaData | null>(() => {
    const adapterNode = nodes.find((e) => e.id === selectedNode) as Node<SchemaData> | undefined

    const internalStatus =
      typeof adapterNode?.data.version === 'number' ? ResourceStatus.LOADED : adapterNode?.data.version

    return adapterNode ? { ...adapterNode.data, internalStatus } : null
  })

  const onReactFlowSchemaFormChange = useCallback(
    (changeEvent: IChangeEvent, id?: string | undefined) => {
      // id have form "root_XXXXXX", which makes the test unsafe
      if (id?.includes('name')) {
        const schema = allSchemas?.items?.findLast((schema) => schema.id === changeEvent.formData.name)
        if (schema) {
          setFormData({
            name: schema.id,
            type: enumFromStringValue(SchemaType, schema.type) || SchemaType.JSON,
            version: schema.version || ResourceStatus.MODIFIED,
            schemaSource: atob(schema.schemaDefinition),
            internalVersions: getSchemaFamilies(allSchemas?.items || [])[schema.id].versions,
            internalStatus: ResourceStatus.LOADED,
          })
        } else {
          setFormData({
            internalStatus: ResourceStatus.DRAFT,
            name: changeEvent.formData.name,
            type: SchemaType.JSON,
            version: ResourceStatus.DRAFT,
            schemaSource: MOCK_JSONSCHEMA_SCHEMA,
          })
        }
      }
      if (id?.includes('type') && formData) {
        if (formData.type !== changeEvent.formData.type) {
          setFormData({
            ...formData,
            type: changeEvent.formData.type,
            schemaSource: changeEvent.formData.type === SchemaType.JSON ? MOCK_JSONSCHEMA_SCHEMA : MOCK_PROTOBUF_SCHEMA,
          })
        }
      }
      if (id?.includes('schemaSource') && formData && formData.internalStatus === ResourceStatus.LOADED) {
        setFormData({
          ...formData,
          version: ResourceStatus.MODIFIED,
          internalStatus: ResourceStatus.MODIFIED,
        })
      }
      if (id?.includes('version') && formData) {
        const schema = allSchemas?.items?.find(
          (schema) => schema.id === formData.name && schema.version?.toString() === changeEvent.formData.version
        )

        if (schema) {
          setFormData({
            ...formData,
            version: changeEvent.formData.version,
            schemaSource: atob(schema.schemaDefinition),
          })
        }
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
    const { internalStatus, internalVersions } = schema || {}
    return {
      // 'ui:order':
      //   internalStatus === ResourceStatus.DRAFT || !internalStatus
      //     ? ['name', 'type', 'schemaSource', 'messageType', 'version']
      //     : ['name', 'version', 'schemaSource', 'messageType', 'type'],
      name: {
        'ui:widget': 'datahub:schema-name',
        'ui:options': {
          isDraft: schema?.version === ResourceStatus.DRAFT,
        },
      },
      version: {
        'ui:options': {
          readonly:
            internalStatus === ResourceStatus.DRAFT || internalStatus === ResourceStatus.MODIFIED || !internalStatus,
          selectOptions: internalVersions,
        },
      },
      type: {
        'ui:options': {
          readonly:
            internalStatus === ResourceStatus.LOADED || internalStatus === ResourceStatus.MODIFIED || !internalStatus,
        },
      },
      schemaSource: {
        'ui:options': {
          readonly: !internalStatus,
        },
      },
    }
  }

  return (
    <Card>
      <CardBody>
        <ReactFlowSchemaForm
          schema={MOCK_SCHEMA_SCHEMA.schema}
          uiSchema={getUISchema(formData)}
          formData={formData}
          widgets={datahubRJSFWidgets}
          customValidate={customValidate}
          onSubmit={onFormSubmit}
          onChange={onReactFlowSchemaFormChange}
        />
      </CardBody>
    </Card>
  )
}
