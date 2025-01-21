import { useEffect, useMemo, useState } from 'react'
import { group } from 'd3-array'
import { hierarchy } from 'd3-hierarchy'

import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import {
  ClusterDataWrapper,
  ClusterFunction,
  groupCatalog,
  TreeEntity,
} from '@/modules/DomainOntology/utils/cluster.utils.ts'

export const useGetClusterData = () => {
  const listBridges = useListBridges()
  const listAdapters = useListProtocolAdapters()
  const [clusterKeys, setClusterKeys] = useState<string[]>([])

  useEffect(() => {
    const interval = setInterval(() => {
      listBridges.refetch().finally()
      listAdapters.refetch().finally()
    }, 2000)
    return () => clearInterval(interval)
  }, [listAdapters, listBridges])

  const data = useMemo(() => {
    const dataSource: ClusterDataWrapper[] = [
      ...(listAdapters.data?.map<ClusterDataWrapper>((e) => ({
        category: TreeEntity.ADAPTER,
        payload: e,
        name: e.id,
      })) || []),
      ...(listBridges.data?.map<ClusterDataWrapper>((e) => ({ category: TreeEntity.BRIDGE, payload: e, name: e.id })) ||
        []),
    ]

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const keyFunctions = clusterKeys.reduce<ClusterFunction<any>[]>((acc, cur) => {
      const clusterKey = groupCatalog.find((key) => key.name === cur)
      if (clusterKey) acc.push(clusterKey.keyFunction)
      return acc
    }, [])

    const clusterGrouped = group(dataSource, ...keyFunctions)

    const clusterHierarchy = hierarchy(clusterGrouped)

    if (clusterHierarchy.children === undefined) {
      return { ...clusterHierarchy, children: [...clusterHierarchy.data], data: ['Root', clusterHierarchy.data] }
    }

    return clusterHierarchy
  }, [clusterKeys, listAdapters.data, listBridges.data])

  return {
    isLoading: listAdapters.isLoading || listBridges.isLoading,
    isError: listAdapters.isError || listBridges.isError,
    data: data,
    clusterKeys,
    setClusterKeys,
  }
}
