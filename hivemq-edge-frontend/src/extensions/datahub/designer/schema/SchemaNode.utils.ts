import type { Connection, Node, NodeAddChange } from '@xyflow/react'
import { parse, util as protobufUtils } from 'protobufjs'
import descriptor from 'protobufjs/ext/descriptor'

import i18n from '@/config/i18n.config.ts'

import type { PolicySchema, SchemaReference, Script } from '@/api/__generated__'
import type { DataHubNodeData, DryRunResults, ResourceFamily, SchemaData } from '@datahub/types.ts'
import { DataHubNodeType, ResourceWorkingVersion, SchemaType } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { enumFromStringValue } from '@/utils/types.utils.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'

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
    // TODO[DATAHUB] Compilation of descriptor a very experimental and outdated solution.
    //    See https://github.com/protobufjs/protobuf.js/tree/master/ext/descriptor
    if (!schemaNode.data.messageType)
      return {
        node: schemaNode,
        error: PolicyCheckErrors.notConfigured(schemaNode, 'messageType'),
      }

    try {
      const root = parse(schemaNode.data.schemaSource).root
      // @ts-ignore No typescript definition
      const MyMessage = root.toDescriptor('proto3')
      const buffer = descriptor.FileDescriptorSet.encode(MyMessage).finish()
      const encoded = protobufUtils.base64.encode(buffer, 0, buffer.length)

      // verifying the double encoding
      const encodedGraphBytes = new Uint8Array(protobufUtils.base64.length(encoded))
      protobufUtils.base64.decode(encoded, encodedGraphBytes, 0)
      const decodedMessage = descriptor.FileDescriptorSet.decode(encodedGraphBytes)
      if (JSON.stringify(MyMessage) !== JSON.stringify(decodedMessage))
        return {
          node: schemaNode,
          error: PolicyCheckErrors.internal(
            schemaNode,
            new Error(i18n.t('datahub:error.validation.protobuf.encoding') as string)
          ),
        }

      const schema: PolicySchema = {
        id: schemaNode.data.name,
        type: schemaNode.data.type,
        schemaDefinition: encoded,
        // TODO[20139] No definition of arguments in OpenAPI!
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

export function loadSchema(
  parentNode: Node<DataHubNodeData>,
  targetHandle: string | null,
  positionInGroup: number,
  schemaRef: SchemaReference,
  schemas: PolicySchema[]
): (NodeAddChange | Connection)[] {
  const schema = schemas.find((schema) => schema.id === schemaRef.schemaId)
  if (!schema)
    throw new Error(i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.SCHEMA }) as string)

  if (schema.type === SchemaType.JSON) {
    const schemaNode: Node<SchemaData> = {
      id: schemaRef.schemaId,
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
    const encodedGraphBytes = new Uint8Array(protobufUtils.base64.length(schema.schemaDefinition))
    protobufUtils.base64.decode(schema.schemaDefinition, encodedGraphBytes, 0)
    const decodedMessage = descriptor.FileDescriptorSet.decode(encodedGraphBytes)
    // @ts-ignore
    const messageTypeDecoded = decodedMessage.file[0].messageType[0].name

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
        schemaSource: i18n.t('datahub:error.validation.protobuf.template', { source: messageTypeDecoded }) as string,
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

  throw new Error(i18n.t('datahub:error.loading.schema.unknown', { type: schema.type }) as string)
}
