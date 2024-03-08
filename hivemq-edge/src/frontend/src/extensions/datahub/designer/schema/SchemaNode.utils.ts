import { Node } from 'reactflow'
import { DryRunResults, SchemaData, SchemaProtobufArguments, SchemaType } from '@datahub/types.ts'
import { Schema } from '@/api/__generated__'
import { parse, Root } from 'protobufjs'

import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export function checkValiditySchema(schemaNode: Node<SchemaData>): DryRunResults<Schema> {
  if (!schemaNode.data.type || !schemaNode.data.version || !schemaNode.data.schemaSource) {
    return {
      node: schemaNode,
      error: PolicyCheckErrors.notConfigured(schemaNode, 'type, version, schemaSource'),
    }
  }

  // TODO[20139] No definition of arguments in OpenAPI!
  let args: SchemaProtobufArguments | undefined = undefined
  if (schemaNode.data.type === SchemaType.PROTOBUF) {
    if (!schemaNode.data.messageType)
      return {
        node: schemaNode,
        error: PolicyCheckErrors.notConfigured(schemaNode, 'messageType'),
      }
    args = { messageType: schemaNode.data.messageType }

    try {
      // @ts-ignore
      const root: Root = parse(schemaNode.data.schemaSource).root
      // TODO[20139] No compilation of PROTOBUF descriptor
    } catch (e) {}
  }

  const schema: Schema = {
    // @ts-ignore TODO[19466] Id should be user-facing; Need to fix before merging!
    id: schemaNode.id,
    type: schemaNode.data.type,
    version: schemaNode.data.version,
    // TODO[20139] No compiled PROTOBUF descriptor. schemaDefinition is wrong
    schemaDefinition: btoa(schemaNode.data.schemaSource),
    arguments: { ...args },
  }
  return { data: schema, node: schemaNode }
}
