import { type FC, useMemo, useState, type MouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { stratify } from 'd3-hierarchy'
import { ButtonGroup, Card, CardBody, CardHeader, FormControl, FormLabel, HStack, Switch } from '@chakra-ui/react'
import { LuFolderTree } from 'react-icons/lu'
import { TbChartDonutFilled } from 'react-icons/tb'

import { useGetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useGetUnifiedNamespace.ts'
import { useSetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useSetUnifiedNamespace.ts'

import IconButton from '@/components/Chakra/IconButton.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'
import SunburstChart from '@/modules/Workspace/components/topics/SunburstChart.tsx'
import TreeViewChart from '@/modules/Workspace/components/topics/TreeViewChart.tsx'
import { useGetTopicSamples } from '@/api/hooks/useTopicOntology/useGetTopicSamples.tsx'

type SUNBURST_CHART = 'nivo' | 'recharts' | 'treeview'

interface TopicSunburstProps {
  onSelect?: (topic: string | undefined) => void
}

const TopicExplorer: FC<TopicSunburstProps> = ({ onSelect }) => {
  const { t } = useTranslation()
  const [chart, setChart] = useState<SUNBURST_CHART>('treeview')
  const { data: uns } = useGetUnifiedNamespace()
  const { mutateAsync } = useSetUnifiedNamespace()
  const [useOrigin, setUseOrigin] = useState(false)
  const { data, isLoading } = useGetEdgeTopics({ publishOnly: false, useOrigin: useOrigin })
  const { data: topicSamples } = useGetTopicSamples()

  const unsPrefix = useMemo(() => {
    if (!uns || !uns.enabled) return ''
    return (
      [uns.enterprise, uns.site, uns.area, uns.productionLine, uns.workCell].filter((e) => Boolean(e)).join('/') + '/'
    )
  }, [uns])

  const treeData = useMemo(() => {
    const metadata = [...data, ...(topicSamples || [])].map<TopicTreeMetadata>((e) => {
      const lb = e.includes('#') ? e : unsPrefix + e
      return { label: lb, count: 1 }
    })
    return stratify<{ label: string; count: number }>().path((d) => d.label)(metadata)
  }, [data, topicSamples, unsPrefix])

  if (isLoading) return <LoaderSpinner />

  const onHandleSelect = (topic: string, event: MouseEvent) => {
    onSelect?.(topic)
    event.stopPropagation()
  }

  return (
    <Card size="sm" w="100%" h="500px">
      <CardHeader as={HStack} gap={10}>
        <ButtonGroup isAttached>
          <IconButton
            icon={<LuFolderTree />}
            aria-label={t('workspace.topicWheel.control.treeview')}
            isDisabled={chart === 'treeview'}
            onClick={() => setChart('treeview')}
          />
          <IconButton
            icon={<TbChartDonutFilled />}
            aria-label={t('workspace.topicWheel.control.sunburst')}
            isDisabled={chart === 'nivo'}
            onClick={() => setChart('nivo')}
          />
        </ButtonGroup>
        <FormControl display="flex" alignItems="center">
          <FormLabel htmlFor="control-uns" mb={0}>
            {t('workspace.topicWheel.control.uns')}
          </FormLabel>
          <Switch
            id="control-uns"
            isChecked={uns?.enabled}
            onChange={() => {
              if (uns) mutateAsync({ requestBody: { ...uns, enabled: !uns.enabled } })
            }}
          />
        </FormControl>
        <FormControl display="flex" alignItems="center">
          <FormLabel htmlFor="control-origin" mb={0}>
            {t('workspace.topicWheel.control.origin')}
          </FormLabel>
          <Switch
            id="control-origin"
            isChecked={useOrigin}
            onChange={() => {
              setUseOrigin((e) => !e)
            }}
          />
        </FormControl>
      </CardHeader>
      <CardBody onClick={() => onSelect?.(undefined)}>
        {chart === 'nivo' && <SunburstChart data={sunburstData} onSelect={onHandleSelect} />}
        {chart === 'treeview' && <TreeViewChart data={sunburstData} onSelect={onSelect} />}
      </CardBody>
    </Card>
  )
}

export default TopicExplorer
