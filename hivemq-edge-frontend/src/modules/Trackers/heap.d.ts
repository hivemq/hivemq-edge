interface IHeap {
  track: (event: string, properties?: object) => void
  identify: (identity: string) => void
  resetIdentity: () => void
  addUserProperties: (properties: object) => void
  addEventProperties: (properties: object) => void
  removeEventProperty: (property: string) => void
  clearEventProperties: () => void
  appid: string
  userId: string
  identity: string | null
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  config: any
  // added to cover the load function
  load: (id: string) => void
}

declare global {
  interface Window {
    heap: IHeap
  }
}

export {}
