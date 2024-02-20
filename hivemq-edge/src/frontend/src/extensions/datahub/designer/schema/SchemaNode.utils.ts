import { Node } from 'reactflow'
import { DryRunResults, SchemaData } from '@datahub/types.ts'
import { Schema } from '@/api/__generated__'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export function checkValiditySchema(schemaNode: Node<SchemaData>): DryRunResults<Schema> {
  if (!schemaNode.data.type || !schemaNode.data.version) {
    return {
      node: schemaNode,
      error: PolicyCheckErrors.notConfigured(schemaNode, 'type, version'),
    }
  }

  const schema: Schema = {
    // @ts-ignore TODO[19466] Id should be user-facing; Need to fix before merging!
    id: schemaNode.id,
    type: schemaNode.data.type,
    // @ts-ignore TODO[19475] Need to fix before merging!
    version: schemaNode.data.version,
    schemaDefinition: btoa(JSON.stringify(schemaNode.data.schemaSource)),
    // TODO[19240] No definition of arguments!
    arguments: {},
  }
  return { data: schema, node: schemaNode }
}
