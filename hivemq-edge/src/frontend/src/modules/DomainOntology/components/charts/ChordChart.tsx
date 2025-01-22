import { FC } from 'react'
import { ResponsiveChord } from '@nivo/chord'
import { Card, CardBody } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

interface ChordChartProps {
  id?: string
  matrix: number[][]
  keys: string[]
}

const ChordChart: FC<ChordChartProps> = ({ matrix, keys }) => {
  const { t } = useTranslation()
  return (
    <ResponsiveChord
      ariaLabel={t('ontology.charts.relationChord.title')}
      data={matrix}
      keys={keys}
      margin={{ top: 0, right: 200, bottom: 0, left: 0 }}
      // valueFormat=".2f"
      padAngle={0.25}
      innerRadiusRatio={0.86}
      innerRadiusOffset={0.02}
      inactiveArcOpacity={0.4}
      arcBorderWidth={0}
      arcBorderColor={{
        from: 'color',
        modifiers: [['darker', 0.4]],
      }}
      activeRibbonOpacity={0.75}
      inactiveRibbonOpacity={0}
      ribbonBorderWidth={0}
      ribbonBorderColor={{
        from: 'color',
        modifiers: [['darker', 0.4]],
      }}
      arcTooltip={({ arc }) => {
        return (
          <Card>
            <CardBody>{arc.label}</CardBody>
          </Card>
        )
      }}
      ribbonTooltip={({ ribbon }) => {
        return (
          <Card>
            <CardBody>
              From {ribbon.target.label} to {ribbon.source.label}
            </CardBody>
          </Card>
        )
      }}
      labelOffset={9}
      labelRotation={-90}
      labelTextColor={{
        from: 'color',
        modifiers: [['darker', 1]],
      }}
      colors={{ scheme: 'category10' }}
      legends={[
        {
          anchor: 'right',
          direction: 'column',
          justify: false,
          translateX: 120,
          translateY: 0,
          itemWidth: 80,
          itemHeight: 11,
          itemsSpacing: 0,
          itemTextColor: '#999',
          itemDirection: 'left-to-right',
          symbolSize: 12,
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

export default ChordChart
