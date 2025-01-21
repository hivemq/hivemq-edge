import { useMemo } from 'react'
import * as d3 from 'd3'
import { Tree } from '@/modules/Ontology/ff.ts'

type HierarchicalEdgeBundlingProps = {
  width: number
  height: number
  data: Tree
}

const BUNDLE_COEFF = 0.95
const MARGIN = 200

export const HierarchicalEdgeBundling = ({ width, height, data }: HierarchicalEdgeBundlingProps) => {
  const hierarchy = useMemo(() => {
    return d3.hierarchy(data).sum((d) => d.value)
  }, [data])

  const radius = Math.min(width, height) / 2 - MARGIN

  const dendrogram = useMemo(() => {
    const dendrogramGenerator = d3
      .cluster<Tree>()
      .size([360, radius])
      .separation((a, b) => {
        return a.parent == b.parent ? 1 : 6
      })
    return dendrogramGenerator(hierarchy)
  }, [radius, hierarchy])

  const allNodes = dendrogram
    .descendants()
    .filter((node) => node.data.type === 'leaf')
    .map((node) => {
      console.log('XXXXXX', node)
      const turnLabelUpsideDown = node.x > 180
      return (
        <g key={`${node.data.name}-source`} transform={'rotate(' + (node.x - 90) + ')translate(' + node.y + ')'}>
          <circle cx={0} cy={0} r={5} stroke="transparent" fill="#69b3a2" />
          {!node.children && (
            <text
              x={turnLabelUpsideDown ? -15 : 15}
              y={0}
              fontSize={12}
              textAnchor={turnLabelUpsideDown ? 'end' : 'start'}
              transform={turnLabelUpsideDown ? 'rotate(180)' : 'rotate(0)'}
              alignmentBaseline="middle"
            >
              {node.data.name}
            </text>
          )}
        </g>
      )
    })

  const linksGenerator = d3
    .lineRadial()
    .radius((d) => d.y)
    .angle((d) => (d.x / 180) * Math.PI)
    .curve(d3.curveBundle.beta(BUNDLE_COEFF))

  // Compute a map from name to node.
  const nameToNodeMap = {}
  dendrogram.descendants().map((node) => {
    nameToNodeMap[node.data.name] = node
  })

  const gradColor = (t) => d3.interpolateRdBu(1 - t)

  const allEdges = dendrogram
    .descendants()
    .filter((node) => node.data.type === 'leaf' && node.data.links.length > 0)
    .map((sourceNode, i) => {
      return sourceNode.data.links.map((targetNodeName: string) => {
        const traversedNodes = sourceNode.path(nameToNodeMap[targetNodeName])

        const traversedCoords = traversedNodes.map((node) => {
          return { x: node.x, y: node.y }
        })

        console.log('XXXXXXX re', targetNodeName, i, sourceNode.data.name)

        return (
          <path
            key={`${sourceNode.data.name}-s-${targetNodeName}`}
            fill="none"
            stroke="red"
            d={linksGenerator(traversedCoords)}
          />
        )
      })
    })

  return (
    <div>
      <svg
        // width={width}
        // height={height}
        viewBox={`0 0 ${height} ${width}`}
        style={{
          height: '100%',
          marginRight: '0px',
          marginLeft: '0px',
        }}
      >
        <g transform={'translate(' + (radius + MARGIN) + ',' + (radius + MARGIN) + ')'}>
          {allEdges}
          {allNodes}
        </g>
      </svg>
    </div>
  )
}
