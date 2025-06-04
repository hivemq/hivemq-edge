export const objectsEqual = <T extends object>(o1: T, o2: T) =>
  Object.keys(o1).length === Object.keys(o2).length &&
  Object.keys(o1).every((p) => o1[p as keyof T] === o2[p as keyof T])

export const arrayContains = <T extends object>(container: T[], o: T) => {
  return container.some((c) => objectsEqual<T>(c, o))
}

export const arrayWithSameObjects =
  <T extends object>(entities: T[]) =>
  (container: T[]) => {
    return entities.every((entity) => arrayContains<T>(container, entity)) &&
      // container.every((entity) => arrayContains<T>(entities, entity))
      container.length === entities.length
      ? container
      : undefined
  }
