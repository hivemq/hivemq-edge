import { FC } from 'react'
import { type LinkTooltipProps, type NodeTooltipProps, ResponsiveTree, type TreeDataProps } from '@nivo/tree'
import { type PropertyAccessor, useTheme } from '@nivo/core'
import { Box, chakra as Chakra } from '@chakra-ui/react'

import logo from '@/assets/edge/05-icon-industrial-hivemq-edge.svg'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
interface TreeChartProps<T = any> {
  data: TreeDataProps<T>
  identity?: PropertyAccessor<TreeDataProps<T>, string> | undefined
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const NodeTooltip: FC<NodeTooltipProps<any>> = ({ node }) => {
  const theme = useTheme()

  return (
    <Box
      style={{
        ...theme.tooltip.container,
        backgroundColor: node.color,
        color: '#ffffff',
      }}
    >
      <Chakra.dl>
        <Chakra.dt>id</Chakra.dt>
        <Chakra.dd>
          <strong>{node.id}</strong>.
        </Chakra.dd>

        <Chakra.dt>path</Chakra.dt>
        <Chakra.dd>
          <strong>
            {node.ancestorIds.join(' > ')} &gt; {node.id}
          </strong>
        </Chakra.dd>

        <Chakra.dt>uid</Chakra.dt>
        <Chakra.dd>
          <strong>{node.uid}</strong>
        </Chakra.dd>
      </Chakra.dl>
    </Box>
  )
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const LinkTooltip: FC<LinkTooltipProps<any>> = ({ link }) => {
  const theme = useTheme()

  return (
    <Box style={theme.tooltip.container}>
      <Chakra.dl>
        <Chakra.dt>id</Chakra.dt>
        <Chakra.dt>
          <strong>{link.id}</strong>
        </Chakra.dt>
        <br />
        <Chakra.dt>source:</Chakra.dt>
        <Chakra.dt>
          <strong>{link.source.id}</strong>
        </Chakra.dt>
        <br />
        <Chakra.dt>target: </Chakra.dt>
        <Chakra.dt>
          <strong>{link.target.id}</strong>
        </Chakra.dt>
      </Chakra.dl>
    </Box>
  )
}

const TreeChart: FC<TreeChartProps> = ({ data, identity }) => {
  return (
    <ResponsiveTree
      data={data}
      identity={identity}
      mode="dendogram"
      activeNodeSize={24}
      inactiveNodeSize={12}
      nodeColor={{ scheme: 'category10' }}
      fixNodeColorAtDepth={1}
      linkThickness={2}
      activeLinkThickness={8}
      inactiveLinkThickness={2}
      orientLabel={false}
      linkColor={{
        from: 'target.color',
        modifiers: [['opacity', 0.4]],
      }}
      margin={{ top: 90, right: 90, bottom: 90, left: 90 }}
      motionConfig="stiff"
      meshDetectionRadius={80}
      layout="bottom-to-top"
      onLinkMouseEnter={() => undefined}
      onLinkMouseMove={() => undefined}
      onLinkMouseLeave={() => undefined}
      onLinkClick={() => undefined}
      linkTooltip={LinkTooltip}
      nodeTooltip={NodeTooltip}
      linkTooltipAnchor="center"
      nodeComponent={(element) => {
        if (element.node.depth === 0)
          return (
            <g>
              <circle
                data-testid={element.node.path.join('.')}
                r="20"
                fill="none"
                cx={element.node.x}
                cy={element.node.y}
              />
              <image href={logo} height="30" width="30" x={element.node.x - 15} y={element.node.y - 15} />
            </g>
          )
        return (
          <circle
            data-testid={element.node.path.join('.')}
            r="6"
            fill={element.node.color}
            cx={element.node.x}
            cy={element.node.y}
          />
        )
      }}
    />
  )
}

export default TreeChart
