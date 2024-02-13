import { Node } from 'reactflow'
import { DryRunResults, SchemaData } from '@datahub/types.ts'
import { Schema } from '@/api/__generated__'

export function checkValiditySchema(schemaNode: Node<SchemaData>): DryRunResults<Schema> {
  if (!schemaNode.data.type || !schemaNode.data.version) {
    return {
      node: schemaNode,
      error: {
        title: schemaNode.type as string,
        status: 404,
        detail: 'The Schema is not valid',
        type: 'datahub.notDefined',
        id: schemaNode.id,
      },
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
