import type { PolicySchema, SchemaList } from '../../../api/__generated__'

export interface PolicySchemaExpanded extends PolicySchema {
  children?: PolicySchema[]
}

export const groupResourceItems = (data: SchemaList | undefined) => {
  if (!data || !data.items || !data.items.length) return []

  // TODO[33008] Update Node to 21; Object.groupBy is not supported in Node 18 or 20
  // const schemasGroupedById = Object.groupBy(data.items, (schema) => schema.id)
  const schemasGroupedById = data.items.reduce<Record<string, PolicySchema[]>>((acc, schema) => {
    if (!acc[schema.id]) {
      acc[schema.id] = []
    }
    acc[schema.id].push(schema)
    return acc
  }, {})

  const schemaGroups = Object.entries(schemasGroupedById).filter(([, schemas]) => schemas !== undefined) as [
    string,
    PolicySchema[],
  ][]

  return schemaGroups.map(([, schemas]) => {
    if (schemas.length === 1) return schemas.at(0) as PolicySchemaExpanded

    const firstSchema = schemas.at(0) as PolicySchema
    const lastSchema = schemas.at(-1) as PolicySchema

    return {
      ...firstSchema,
      version: lastSchema.version, // replace the version with the latest one
      children: schemas,
    } as PolicySchemaExpanded
  })
}
