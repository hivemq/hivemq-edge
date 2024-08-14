import { type FC, type MouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ResponsiveSunburst, type ComputedDatum, type SunburstCustomLayerProps } from '@nivo/sunburst'
import { type HierarchyNode } from 'd3-hierarchy'
import { Badge, HStack } from '@chakra-ui/react'

import Topic from '@/components/MQTT/Topic.tsx'
import { TopicTreeMetadata } from '@/modules/Workspace/types.ts'

interface SunburstNivoProps {
  data: HierarchyNode<TopicTreeMetadata>
  onSelect?: (topic: string, event: MouseEvent) => void
}

const TopicTooltip = (props: ComputedDatum<unknown>) => {
  const { id } = props

  return (
    <HStack>
      <Topic topic={id.toString()} />
      <Badge colorScheme="blue">{props.value}</Badge>
    </HStack>
  )
}

const CenteredMetric: FC<SunburstCustomLayerProps<unknown>> = ({ centerX, centerY }) => {
  const { t } = useTranslation()
  // const total = nodes.reduce((total, datum) => total + datum.value, 0)
  return (
    <>
      <text x={centerX} y={centerY} textAnchor="middle" dominantBaseline="central">
        {t('branding.appName')}
      </text>
    </>
  )
}

/**
 * TODO[25055] The Nivo widget is interactive (onSelect) but not accessible (lacks tab-index and relevant aria attributes)
 */
const SunburstChart: FC<SunburstNivoProps> = ({ data, onSelect }) => {
  return (
    <ResponsiveSunburst
      data={data}
      margin={{ top: 0, right: 0, bottom: 0, left: 0 }}
      id="id"
      value="data.count"
      cornerRadius={25}
      borderWidth={2}
      // TODO[25055] Import from ChakraUI theme
      colors={{ scheme: 'category10' }}
      childColor={{
        from: 'color',
        modifiers: [['brighter', 0.15]],
      }}
      enableArcLabels={true}
      arcLabelsSkipAngle={0}
      arcLabelsTextColor="black"
      arcLabel={(e) => e.id.toString().split('/').slice(-1).join('')}
      tooltip={TopicTooltip}
      isInteractive={true}
      onClick={(data, evt) => {
        onSelect?.(data.id.toString(), evt)
      }}
      layers={['arcs', 'arcLabels', CenteredMetric]}
    />
  )
}

export default SunburstChart
