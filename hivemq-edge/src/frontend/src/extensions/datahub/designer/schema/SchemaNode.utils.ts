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
    // @ts-ignore TODO[NVL] Need to fix before merging!
    id: schemaNode.data.name,
    type: schemaNode.data.type,
    // @ts-ignore TODO[NVL] Need to fix before merging!
    version: schemaNode.data.version,
  }
  return { data: schema, node: schemaNode }
}
