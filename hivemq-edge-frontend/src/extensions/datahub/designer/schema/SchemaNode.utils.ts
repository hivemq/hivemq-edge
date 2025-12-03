import type { Connection, Node, NodeAddChange } from '@xyflow/react'

import i18n from '@/config/i18n.config.ts'
import { encodeProtobufSchema, decodeProtobufSchema } from '@datahub/utils/protobuf.utils.ts'

import type { PolicySchema, SchemaReference, Script } from '@/api/__generated__'
import type {
  DataHubNodeData,
  DryRunResults,
  PolicyOperationArguments,
  ResourceFamily,
  SchemaData,
} from '@datahub/types.ts'
import { DataHubNodeType, ResourceWorkingVersion, SchemaType } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { enumFromStringValue } from '@/utils/types.utils.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'
import { SCRIPT_FUNCTION_LATEST } from '@datahub/utils/datahub.utils.ts'
import { getNodeId } from '@datahub/utils/node.utils.ts'

export const getScriptFamilies = (items: Script[]) => {
  return items.reduce<Record<string, ResourceFamily>>((acc, script) => {
    if (acc[script.id]) {
      if (script.version) acc[script.id].versions.push(script.version)
    } else {
      acc[script.id] = { name: script.id, versions: [], type: script.functionType }
      if (script.version) acc[script.id].versions.push(script.version)
    }
    return acc
  }, {})
}

export const getSchemaFamilies = (items: PolicySchema[]) => {
  return items.reduce<Record<string, ResourceFamily>>((acc, schema) => {
    if (acc[schema.id]) {
      if (schema.version) acc[schema.id].versions.push(schema.version)
    } else {
      let description: string | undefined
      try {
        const schemaDefinition = JSON.parse(atob(schema.schemaDefinition))
        description = schemaDefinition.description
      } catch (e) {
        /* empty */
      }
      acc[schema.id] = { name: schema.id, versions: [], type: schema.type, description }
      if (schema.version) acc[schema.id].versions.push(schema.version)
    }
    return acc
  }, {})
}

export function checkValiditySchema(schemaNode: Node<SchemaData>): DryRunResults<PolicySchema> {
  if (!schemaNode.data.type || !schemaNode.data.version || !schemaNode.data.schemaSource) {
    return {
      node: schemaNode,
      error: PolicyCheckErrors.notConfigured(schemaNode, 'type, version, schemaSource'),
    }
  }

  if (schemaNode.data.type === SchemaType.JSON) {
    const jsonSchema: PolicySchema = {
      id: schemaNode.data.name,
      type: schemaNode.data.type,
      schemaDefinition: btoa(schemaNode.data.schemaSource),
    }
    return { data: jsonSchema, node: schemaNode }
  }

  if (schemaNode.data.type === SchemaType.PROTOBUF) {
    if (!schemaNode.data.messageType)
      return {
        node: schemaNode,
        error: PolicyCheckErrors.notConfigured(schemaNode, 'messageType'),
      }

    try {
      const encoded = encodeProtobufSchema(schemaNode.data.schemaSource)

      const schema: PolicySchema = {
        id: schemaNode.data.name,
        type: schemaNode.data.type,
        schemaDefinition: encoded,
        arguments: { messageType: schemaNode.data.messageType },
      }
      return { data: schema, node: schemaNode }
    } catch (e) {
      return {
        node: schemaNode,
        error: PolicyCheckErrors.internal(schemaNode, e),
      }
    }
  }

  return {
    node: schemaNode,
    error: PolicyCheckErrors.notConfigured(schemaNode, 'schemaSource'),
  }
}

/**
 * Get the version of a schema reference from either the validator or the argument of the script operation.
 * TODO[20139] Remove the PolicyOperationArguments type when the OpenAPI specs are updated. Both should use the same type.
 * @param schemaRef
 */
export const getSchemaRefVersion = (schemaRef: SchemaReference | PolicyOperationArguments): string => {
  return (schemaRef as SchemaReference).version || (schemaRef as PolicyOperationArguments).schemaVersion
}

export function loadSchema(
  parentNode: Node<DataHubNodeData>,
  targetHandle: string | null,
  positionInGroup: number,
  schemaRef: SchemaReference | PolicyOperationArguments,
  schemas: PolicySchema[]
): (NodeAddChange | Connection)[] {
  const schemaFamily = schemas.filter((schema) => schema.id === schemaRef.schemaId)
  if (!schemaFamily.length)
    throw new Error(i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.SCHEMA }) as string)

  let schema: PolicySchema | undefined
  const version = getSchemaRefVersion(schemaRef)
  if (version === SCRIPT_FUNCTION_LATEST) {
    schema = schemaFamily.slice(-1)[0]
  } else {
    schema = schemaFamily.find((s) => s.version?.toString() === version)
  }

  if (!schema)
    throw new Error(i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.SCHEMA }) as string)

  if (schema.type === SchemaType.JSON) {
    const schemaNode: Node<SchemaData> = {
      id: getNodeId(DataHubNodeType.SCHEMA),
      type: DataHubNodeType.SCHEMA,
      position: {
        x: parentNode.position.x + CANVAS_POSITION.PolicySchema.x,
        y: parentNode.position.y + positionInGroup * CANVAS_POSITION.SchemaOperation.y,
      },
      data: {
        name: schema.id,
        // @ts-ignore force undefined
        type: enumFromStringValue(SchemaType, schema.type),
        schemaSource: atob(schema.schemaDefinition),
        version: schema.version || ResourceWorkingVersion.DRAFT,
        internalVersions: schema.version ? [schema.version] : undefined,
      },
    }

    return [
      { item: schemaNode, type: 'add' },
      {
        source: schemaNode.id,
        target: parentNode.id,
        sourceHandle: null,
        targetHandle: targetHandle,
      },
    ]
  }

  if (schema.type === SchemaType.PROTOBUF) {
    let schemaSource: string
    try {
      schemaSource = decodeProtobufSchema(schema.schemaDefinition)
    } catch (e) {
      // Fallback to error message if decoding fails
      schemaSource = i18n.t('datahub:error.validation.protobuf.template', { source: 'UNKNOWN' }) as string
    }

    const schemaNode: Node<SchemaData> = {
      id: schemaRef.schemaId,
      type: DataHubNodeType.SCHEMA,
      position: {
        x: parentNode.position.x + positionInGroup,
        y: parentNode.position.y + CANVAS_POSITION.PolicySchema.y,
      },
      data: {
        // @ts-ignore force undefined
        type: enumFromStringValue(SchemaType, schema.type),
        schemaSource,
        version: 1,
      },
    }

    return [
      { item: schemaNode, type: 'add' },
      {
        source: schemaNode.id,
        target: parentNode.id,
        sourceHandle: null,
        targetHandle: targetHandle,
      },
    ]
  }

  throw new Error(i18n.t('datahub:error.loading.schema.unknown', { type: schema.type }))
}

export const getSourceFromSchema = (schema: PolicySchema) => {
  const schemaType = enumFromStringValue(SchemaType, schema.type) || SchemaType.JSON

  // Decode schema source based on type
  let schemaSource: string
  if (schemaType === SchemaType.PROTOBUF) {
    try {
      schemaSource = decodeProtobufSchema(schema.schemaDefinition)
    } catch (e) {
      schemaSource = i18n.t('datahub:error.validation.protobuf.decoding', {
        error: e instanceof Error ? e.message : String(e),
      })
    }
  } else {
    schemaSource = atob(schema.schemaDefinition)
  }
  return schemaSource
}
