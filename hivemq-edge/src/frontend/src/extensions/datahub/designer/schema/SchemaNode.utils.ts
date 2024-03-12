import { Node, NodeAddChange } from 'reactflow'
import { parse, util as protobufUtils } from 'protobufjs'
import descriptor from 'protobufjs/ext/descriptor'

import i18n from '@/config/i18n.config.ts'

import { Schema, SchemaReference } from '@/api/__generated__'
import {
  DataHubNodeData,
  DataHubNodeType,
  DryRunResults,
  SchemaData,
  SchemaType,
  WorkspaceAction,
  WorkspaceState,
} from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { enumFromStringValue } from '@/utils/types.utils.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'

export function checkValiditySchema(schemaNode: Node<SchemaData>): DryRunResults<Schema> {
  if (!schemaNode.data.type || !schemaNode.data.version || !schemaNode.data.schemaSource) {
    return {
      node: schemaNode,
      error: PolicyCheckErrors.notConfigured(schemaNode, 'type, version, schemaSource'),
    }
  }

  if (schemaNode.data.type === SchemaType.JSON) {
    const jsonSchema: Schema = {
      // TODO[19466] Id should be user-facing; Need to fix before merging!
      id: schemaNode.id,
      type: schemaNode.data.type,
      version: schemaNode.data.version,
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

      const schema: Schema = {
        // @ts-ignore TODO[19466] Id should be user-facing; Need to fix before merging!
        id: schemaNode.id,
        type: schemaNode.data.type,
        version: schemaNode.data.version,
        schemaDefinition: encoded,
        // TODO[20139] No definition of arguments in OpenAPI!
        arguments: { messageType: schemaNode.data.messageType },
      }
      return { data: schema, node: schemaNode }
    } catch (e) {
      console.log(e)
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
  positionDeltaX: number,
  schemaRef: SchemaReference,
  schemas: Schema[],
  store: WorkspaceState & WorkspaceAction
) {
  const { onNodesChange, onConnect } = store
  const schema = schemas.find((schema) => schema.id === schemaRef.schemaId)
  if (!schema)
    throw new Error(i18n.t('datahub:error.loading.connection.notFound', { source: DataHubNodeType.SCHEMA }) as string)

  if (schema.type === SchemaType.JSON) {
    const schemaNode: Node<SchemaData> = {
      id: schemaRef.schemaId,
      type: DataHubNodeType.SCHEMA,
      position: {
        x: parentNode.position.x + positionDeltaX,
        y: parentNode.position.y + CANVAS_POSITION.Schema.y,
      },
      data: {
        // @ts-ignore force undefined
        type: enumFromStringValue(SchemaType, schema.type),
        schemaSource: atob(schema.schemaDefinition),
        version: 1,
      },
    }
    onNodesChange([{ item: schemaNode, type: 'add' } as NodeAddChange])
    onConnect({
      source: schemaNode.id,
      target: parentNode.id,
      sourceHandle: null,
      targetHandle: targetHandle,
    })
  } else if (schema.type === SchemaType.PROTOBUF) {
    const encodedGraphBytes = new Uint8Array(protobufUtils.base64.length(schema.schemaDefinition))
    protobufUtils.base64.decode(schema.schemaDefinition, encodedGraphBytes, 0)
    const decodedMessage = descriptor.FileDescriptorSet.decode(encodedGraphBytes)
    // @ts-ignore
    const messageTypeDecoded = decodedMessage.file[0].messageType[0].name

    const schemaNode: Node<SchemaData> = {
      id: schemaRef.schemaId,
      type: DataHubNodeType.SCHEMA,
      position: {
        x: parentNode.position.x + positionDeltaX,
        y: parentNode.position.y + CANVAS_POSITION.Schema.y,
      },
      data: {
        // @ts-ignore force undefined
        type: enumFromStringValue(SchemaType, schema.type),
        schemaSource: i18n.t('datahub:error.validation.protobuf.template', { source: messageTypeDecoded }) as string,
        version: 1,
      },
    }
    onNodesChange([{ item: schemaNode, type: 'add' } as NodeAddChange])
    onConnect({
      source: schemaNode.id,
      target: parentNode.id,
      sourceHandle: null,
      targetHandle: targetHandle,
    })
  } else throw new Error(i18n.t('datahub:error.loading.schema.unknown', { type: schema.type }) as string)
}
