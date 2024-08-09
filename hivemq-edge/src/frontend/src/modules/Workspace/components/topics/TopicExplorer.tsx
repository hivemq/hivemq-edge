import { type FC, useMemo, useState } from 'react'
import { stratify } from 'd3-hierarchy'
import { ButtonGroup, Card, CardBody, CardHeader, FormControl, FormLabel, HStack, Switch } from '@chakra-ui/react'
import { LuFolderTree } from 'react-icons/lu'
import { TbChartDonutFilled } from 'react-icons/tb'
import { PiChartDonutFill } from 'react-icons/pi'

import { useGetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useGetUnifiedNamespace.ts'
import { useSetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useSetUnifiedNamespace.ts'

import IconButton from '@/components/Chakra/IconButton.tsx'
import { type TopicTreeMetadata } from '@/modules/Workspace/types.ts'
import SunburstNivo from '@/modules/Workspace/components/topics/SunburstNivo.tsx'
import SunburstReCharts from '@/modules/Workspace/components/topics/SunburstReCharts.tsx'
import TreeView from '@/modules/Workspace/components/topics/TreeView.tsx'

type SUNBURST_CHART = 'nivo' | 'recharts' | 'treeview'

interface TopicSunburstProps {
  onSelect?: (topic: string) => void
  // data: HierarchyNode<any>
  data: string[]
}

const TopicExplorer: FC<TopicSunburstProps> = ({ data }) => {
  const [chart, setChart] = useState<SUNBURST_CHART>('treeview')
  const { data: uns } = useGetUnifiedNamespace()
  const { mutateAsync } = useSetUnifiedNamespace()

  const unsPrefix = useMemo(() => {
    if (!uns || !uns.enabled) return ''
    return (
      [uns.enterprise, uns.site, uns.area, uns.productionLine, uns.workCell].filter((e) => Boolean(e)).join('.') + '/'
    )
  }, [uns])

  const treeData = useMemo(() => {
    const metadata = data.map<TopicTreeMetadata>((e) => {
      const lb = e === '#' ? e : unsPrefix + e
      return { label: lb, count: 1 }
    })
    return stratify<{ label: string; count: number }>().path((d) => d.label)(metadata)
  }, [data, unsPrefix])

  return (
    <Card size="sm" w="100%" h="100%">
      <CardHeader as={HStack} gap={10}>
        <ButtonGroup isAttached>
          <IconButton
            icon={<LuFolderTree />}
            aria-label="ffdfd"
            isDisabled={chart === 'treeview'}
            onClick={() => setChart('treeview')}
          />
          <IconButton
            icon={<TbChartDonutFilled />}
            aria-label="ffdfd"
            isDisabled={chart === 'nivo'}
            onClick={() => setChart('nivo')}
          />
          <IconButton
            icon={<PiChartDonutFill />}
            aria-label="ffdfd"
            isDisabled={chart === 'recharts'}
            onClick={() => setChart('recharts')}
          />
        </ButtonGroup>
        <FormControl display="flex" alignItems="center">
          <FormLabel htmlFor="email-alerts" mb="0">
            Use UNS
          </FormLabel>
          <Switch
            id="email-alerts"
            isChecked={uns?.enabled}
            onChange={() => {
              if (!uns) return
              mutateAsync({ requestBody: { ...uns, enabled: !uns.enabled } })
            }}
          />
        </FormControl>
      </CardHeader>
      <CardBody>
        {chart === 'nivo' && <SunburstNivo data={treeData} />}
        {chart === 'recharts' && <SunburstReCharts data={treeData} />}
        {chart === 'treeview' && <TreeView data={treeData} />}
      </CardBody>
    </Card>
  )
}

export default TopicExplorer
