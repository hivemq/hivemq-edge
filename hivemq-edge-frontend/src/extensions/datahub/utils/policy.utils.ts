import type { PolicySchema, SchemaList } from '../../../api/__generated__'

export interface PolicySchemaExpanded extends PolicySchema {
  children?: PolicySchema[]
}

export const groupResourceItems = (data: SchemaList | undefined) => {
  if (!data || !data.items) return []

  const schemasGroupedById = Object.groupBy(data.items, (schema) => schema.id)
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
