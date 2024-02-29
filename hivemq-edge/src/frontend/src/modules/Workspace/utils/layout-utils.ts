import { Node, XYPosition } from 'reactflow'
import { group } from 'd3-array'
import { hierarchy, pack } from 'd3-hierarchy'
import { DateTime } from 'luxon'

import { EdgeFlowGrouping, EdgeFlowLayout, IdStubs, NodeTypes } from '@/modules/Workspace/types.ts'
import { Adapter, Bridge } from '@/api/__generated__'

import { CONFIG_ADAPTER_WIDTH } from './nodes-utils.ts'

type ClusterFunction = (d: Node<Adapter>) => string | boolean | number | undefined
interface ClusterFunctionCatalog {
  key: string
  function: ClusterFunction
}
export const groupingAttributes: ClusterFunctionCatalog[] = [
  { key: 'type', function: (d: Node<Adapter>) => d.data.type },
  {
    key: 'subscriptionCardinality',
    function: (d: Node<Adapter>) => !!d.data.config?.subscriptions.length,
  },
  {
    key: 'runtimeDuration',
    function: (d: Node<Adapter>) => {
      // TODO[NVL] Can we breakt time down in unit groups ?
      const gg = d.data.status?.startedAt

      if (!gg) return 0
      const ss = DateTime.fromISO(gg).toMillis()
      return ss % 3
    },
  },
]

const groupingAttributesAsObject = groupingAttributes.reduce<{ [key: string]: ClusterFunction }>(
  (a, c) => ({ ...a, [c.key]: c.function }),
  {}
)

export const applyLayout = (nodes: Node<Bridge | Adapter>[], groupOption: EdgeFlowGrouping): Node[] => {
  if (groupOption.layout === EdgeFlowLayout.CIRCLE_PACKING) return computeCirclePacking(nodes, groupOption)
  return nodes
}

export const computeCirclePacking = (nodes: Node<Bridge | Adapter>[], groupOption: EdgeFlowGrouping): Node[] => {
  const allAdapters = nodes.filter((e) => e.type === NodeTypes.ADAPTER_NODE) as Node<Adapter>[]
  const groupKeys = [
    // This key is mandatory in order to handle adapter grouping
    (d: Node<Adapter>) => d.type,
    ...groupOption.keys.map((e) => groupingAttributesAsObject[e]),
    // groupingAttributes[1].function,
    // groupingAttributes[1].function,
    // groupingAttributes[2].function,
  ] as const
  // @ts-ignore
  const groups = group(allAdapters, ...groupKeys)

  const groupedAdapters = groups.get(NodeTypes.ADAPTER_NODE)

  const workspaceLayout = pack()
    .size([2000, 1000])
    .padding(10)
    .radius(() => CONFIG_ADAPTER_WIDTH)

  const hierarchyAdapters = hierarchy(groupedAdapters)

  const root = workspaceLayout(
    // @ts-ignore
    hierarchyAdapters
      .sum(() => 1)
      .sort((a, b) => {
        return (b.value || 0) - (a.value || 0)
      })
  )

  // @ts-ignore
  const mapping = root.leaves().reduce((a, v) => ({ ...a, [v.data.id]: { x: v.x, y: v.y } }), {})

  const reloc = nodes.map<Node>((e) => {
    // @ts-ignore
    const pos: XYPosition = mapping[e.id]
    if (!pos) return e
    return { ...e, position: { x: pos.x - 600 - 60, y: pos.y - 800 - 40 } }
  })

  const createGroupNodes = () => {
    if (!groupOption.showGroups) return []

    const grpNodes: Node<string>[] =
      root.children?.map<Node>((e, n) => {
        e.leaves().forEach((e) => {
          // @ts-ignore
          e.data.parentNode = `${IdStubs.GROUP_NODE}-${n}`
        })
        return {
          id: `${IdStubs.GROUP_NODE}-${n}`,
          type: NodeTypes.CLUSTER_NODE,
          data: `${IdStubs.GROUP_NODE}-${n}`,
          position: { x: e.x - 600 - e.r, y: e.y - 800 - e.r },
          style: {
            width: 2 * e.r,
            height: 2 * e.r,
          },
        }
      }) || []

    return grpNodes
  }

  return [...createGroupNodes(), ...reloc]
}
