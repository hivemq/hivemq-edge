import { FC } from 'react'
import { ResponsiveSankey, SankeyDataProps, DefaultNode, DefaultLink } from '@nivo/sankey'

interface SankeyChartProps {
  data: SankeyDataProps<DefaultNode, DefaultLink>
}

const SankeyChart: FC<SankeyChartProps> = ({ data }) => {
  return (
    <ResponsiveSankey
      data={data.data}
      margin={{ top: 0, right: 200, bottom: 0, left: 0 }}
      align="justify"
      colors={{ scheme: 'category10' }}
      nodeOpacity={1}
      nodeHoverOthersOpacity={0.35}
      nodeThickness={18}
      nodeSpacing={24}
      nodeBorderWidth={0}
      nodeBorderColor={{
        from: 'color',
        modifiers: [['darker', 0.8]],
      }}
      nodeBorderRadius={3}
      linkOpacity={0.5}
      linkHoverOthersOpacity={0.1}
      linkContract={3}
      enableLinkGradient={true}
      labelPosition="inside"
      labelOrientation="horizontal"
      labelPadding={16}
      labelTextColor={{
        from: 'color',
        modifiers: [['darker', 1]],
      }}
      legends={[
        {
          anchor: 'bottom-right',
          direction: 'column',
          translateX: 180,
          itemWidth: 100,
          itemHeight: 14,
          itemDirection: 'right-to-left',
          itemsSpacing: 2,
          itemTextColor: '#999',
          symbolSize: 14,
          effects: [
            {
              on: 'hover',
              style: {
                itemTextColor: '#000',
              },
            },
          ],
        },
      ]}
    />
  )
}

export default SankeyChart
