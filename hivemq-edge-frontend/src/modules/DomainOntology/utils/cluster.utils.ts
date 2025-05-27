import { DateTime } from 'luxon'
import type { Adapter, Bridge } from '@/api/__generated__'

export enum TreeEntity {
  BRIDGE = 'BRIDGE',
  ADAPTER = 'ADAPTER',
}

export interface ClusterDataWrapper {
  category: TreeEntity
  name: string
  payload: Adapter | Bridge
}

export type ClusterFunction<T> = (d: T) => string | boolean | number | undefined

export interface ClusterFunctionCatalog<T> {
  key: string
  name: string
  description?: string
  keyFunction: ClusterFunction<T>
}

// utils for cluster key functions
// const ggg = dataSource.map((e) => DateTime.fromISO(e.payload.status?.startedAt).toRelative())
// const scale = scaleCluster().domain(ggg).range(['1', '2', '3', '4', '5'])

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const groupCatalog: ClusterFunctionCatalog<any>[] = [
  { key: 'runtime', name: 'Runtime Status', keyFunction: (e) => e.payload.status?.runtime },
  {
    key: 'startedAt',
    name: 'Started at',
    keyFunction: (e) => DateTime.fromISO(e.payload.status?.startedAt).toRelative(),
  },
  { key: 'type', name: 'Adapter type', keyFunction: (e) => e.payload.type },
  { key: 'cat', name: 'ssss', keyFunction: (e) => e.cat },
]
