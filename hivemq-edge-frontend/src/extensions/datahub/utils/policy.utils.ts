// TODO[OpenAPI] This MUST be the common properties of DataHub resources, i.e. Script and Schema
export interface ResourceBase {
  id: string
  readonly version?: number
}

export type ExpandableGroupedResource<T extends ResourceBase> = T & {
  children?: T[]
}

export const groupResourceItems = <T extends { items?: Array<U> }, U extends ResourceBase>(data: T | undefined) => {
  if (!data || !data.items || !data.items.length) return []

  // TODO[33008] Update Node to 21; Object.groupBy is not supported in Node 18 or 20
  // const schemasGroupedById = Object.groupBy(data.items, (schema) => schema.id)
  const schemasGroupedById = data.items.reduce<Record<string, U[]>>((acc, schema) => {
    if (!acc[schema.id]) {
      acc[schema.id] = []
    }
    acc[schema.id].push(schema)
    return acc
  }, {})

  const schemaGroups = Object.entries(schemasGroupedById).filter(([, schemas]) => schemas !== undefined) as [
    string,
    U[],
  ][]

  return schemaGroups.map(([, schemas]) => {
    if (schemas.length === 1) return schemas.at(0) as ExpandableGroupedResource<U>

    const firstSchema = schemas.at(0) as U
    const lastSchema = schemas.at(-1) as U

    return {
      ...firstSchema,
      version: lastSchema.version, // replace the version with the latest one
      children: schemas,
    } as ExpandableGroupedResource<U>
  })
}
