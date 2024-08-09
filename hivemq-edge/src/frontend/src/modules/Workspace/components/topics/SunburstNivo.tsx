import { FC } from 'react'
import { ComputedDatum, ResponsiveSunburst } from '@nivo/sunburst'
import { HierarchyNode } from 'd3-hierarchy'
import { Badge, HStack } from '@chakra-ui/react'
import Topic from '@/components/MQTT/Topic.tsx'

interface SunburstNivoProps {
  data: HierarchyNode<any>
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

const SunburstNivo: FC<SunburstNivoProps> = ({ data }) => {
  return (
    <ResponsiveSunburst
      data={data}
      margin={{ top: 0, right: 0, bottom: 0, left: 0 }}
      id="id"
      value="data.count"
      cornerRadius={25}
      borderWidth={2}
      // borderColor={{ theme: 'background' }}
      colors={{ scheme: 'accent' }}
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
    />
  )
}

export default SunburstNivo
