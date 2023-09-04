import { Node, XYPosition } from 'reactflow'
import { group } from 'd3-array'
import { hierarchy, pack } from 'd3-hierarchy'
import { DateTime } from 'luxon'

import { EdgeFlowGrouping, NodeTypes } from '@/modules/EdgeVisualisation/types.ts'
import { Adapter, Bridge } from '@/api/__generated__'

export const groupingAttributes = [
  { name: 'Type', key: (d: Node<Adapter>) => d.data.type },
  { name: 'Subscription count', key: (d: Node<Adapter>) => !!d.data.config?.subscriptions.length },
  {
    name: 'Runtime duration',
    key: (d: Node<Adapter>) => {
      // TODO[NVL] Can we breakt time down in unit groups ?
      const gg = d.data.adapterRuntimeInformation?.lastStartedAttemptTime

      if (!gg) return 0
      const ss = DateTime.fromISO(gg).toMillis()
      return ss % 3
    },
  },
]

export const applyLayout = (nodes: Node<Bridge | Adapter>[], groupOption: EdgeFlowGrouping): Node[] => {
  // TODO[NVL] Any other layout we might want to try ?
  return computeCirclePacking(nodes, groupOption)
}

export const computeCirclePacking = (nodes: Node<Bridge | Adapter>[], groupOption: EdgeFlowGrouping): Node[] => {
  const allAdapters = nodes.filter((e) => e.type === NodeTypes.ADAPTER_NODE) as Node<Adapter>[]
  const groupKeys = [
    (d: Node<Adapter>) => d.type,
    groupingAttributes[0].key,
    // groupingAttributes[1].key,
    // groupingAttributes[2].key,
  ] as const
  const groups = group(allAdapters, ...groupKeys)

  const groupedAdapters = groups.get(NodeTypes.ADAPTER_NODE)

  console.log('XXXXXXX groups', groups)

  const workspaceLayout = pack()
    .size([2000, 1000])
    .padding(20)
    .radius(() => 60)

  const hierarchyAdapters = hierarchy(groupedAdapters)

  const root = workspaceLayout(
    // @ts-ignore
    hierarchyAdapters
      .sum(() => 1)
      .sort((a, b) => {
        return (b.value || 0) - (a.value || 0)
      })
  )

  console.log('XXXXXXX root', root.leaves())

  const mapping = root.leaves().reduce((a, v) => ({ ...a, [v.data.id]: { x: v.x, y: v.y } }), {})

  const reloc = nodes.map<Node>((e) => {
    const pos: XYPosition = mapping[e.id]
    if (!pos) return e
    return { ...e, position: { x: pos.x - 600 - 60, y: pos.y - 800 - 40 } }
  })

  const createGroupNodes = () => {
    if (!groupOption.showGroups) return []

    const grpNodes =
      root.children?.map<Node>((e, n) => {
        e.leaves().forEach((e) => {
          e.data.parentNode = `AAAAA${n}`
        })
        return {
          id: `AAAAA${n}`,
          type: NodeTypes.CLUSTER_NODE,
          data: { label: `AAAAA${n}` },
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
