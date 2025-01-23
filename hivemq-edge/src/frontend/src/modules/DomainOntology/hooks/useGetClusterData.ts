import { useEffect, useMemo, useState } from 'react'
import { group } from 'd3-array'
import { hierarchy } from 'd3-hierarchy'
import { useTranslation } from 'react-i18next'

import { useListBridges } from '@/api/hooks/useGetBridges/useListBridges.ts'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'
import {
  ClusterDataWrapper,
  ClusterFunction,
  groupCatalog,
  TreeEntity,
} from '@/modules/DomainOntology/utils/cluster.utils.ts'

export const useGetClusterData = () => {
  const { t } = useTranslation()
  const listBridges = useListBridges()
  const listAdapters = useListProtocolAdapters()
  const [clusterKeys, setClusterKeys] = useState<string[]>([])

  useEffect(() => {
    if (listBridges.isError || listAdapters.isError) return

    const interval = setInterval(() => {
      listBridges.refetch().finally()
      listAdapters.refetch().finally()
    }, 2000)
    return () => clearInterval(interval)
  }, [listAdapters, listBridges])

  const data = useMemo(() => {
    const emptyStateData = [{ id: t('ontology.error.noDataLoaded') }]

    const dataSource: ClusterDataWrapper[] = [
      ...(listAdapters.data?.map<ClusterDataWrapper>((adapter) => ({
        category: TreeEntity.ADAPTER,
        payload: adapter,
        name: adapter.id,
      })) || []),
      ...(listBridges.data?.map<ClusterDataWrapper>((bridge) => ({
        category: TreeEntity.BRIDGE,
        payload: bridge,
        name: bridge.id,
      })) || []),
    ]

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const keyFunctions = clusterKeys.reduce<ClusterFunction<any>[]>((acc, cur) => {
      const clusterKey = groupCatalog.find((key) => key.name === cur)
      if (clusterKey) acc.push(clusterKey.keyFunction)
      return acc
    }, [])

    const clusterGrouped = group(dataSource, ...keyFunctions)

    const clusterHierarchy = hierarchy(clusterGrouped.length ? clusterGrouped : emptyStateData)

    if (clusterHierarchy.children === undefined) {
      return { ...clusterHierarchy, children: [...clusterHierarchy.data], data: ['Root', clusterHierarchy.data] }
    }

    return clusterHierarchy
  }, [clusterKeys, listAdapters.data, listBridges.data, t])

  return {
    isLoading: listAdapters.isLoading || listBridges.isLoading,
    isError: listAdapters.isError || listBridges.isError,
    data: data,
    clusterKeys,
    setClusterKeys,
  }
}
