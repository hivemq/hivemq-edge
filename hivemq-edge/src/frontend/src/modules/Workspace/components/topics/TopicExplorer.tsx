import { type FC, useMemo, useState, type MouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Card,
  CardBody,
  CardHeader,
  FormControl,
  FormLabel,
  HStack,
  Switch,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
} from '@chakra-ui/react'

import { useGetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useGetUnifiedNamespace.ts'
import { useSetUnifiedNamespace } from '@/api/hooks/useUnifiedNamespace/useSetUnifiedNamespace.ts'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'
import SunburstChart from '@/modules/Workspace/components/topics/SunburstChart.tsx'
import TreeViewChart from '@/modules/Workspace/components/topics/TreeViewChart.tsx'
import { useGetTopicSamples } from '@/api/hooks/useTopicOntology/useGetTopicSamples.tsx'
import { stratifyTopicTree, toTreeMetadata } from '@/modules/Workspace/utils/topics-utils.ts'

interface TopicSunburstProps {
  onSelect?: (topic: string | undefined) => void
}

const TopicExplorer: FC<TopicSunburstProps> = ({ onSelect }) => {
  const { t } = useTranslation()
  const [useOrigin, setUseOrigin] = useState(false)
  const { mutateAsync } = useSetUnifiedNamespace()
  const { data: uns } = useGetUnifiedNamespace()
  const { data: topics, isLoading } = useGetEdgeTopics({ publishOnly: false, useOrigin: useOrigin })
  const { data: topicSamples } = useGetTopicSamples()

  const unsPrefix = useMemo(() => {
    if (!uns || !uns.enabled) return ''
    return (
      [uns.enterprise, uns.site, uns.area, uns.productionLine, uns.workCell]
        .filter((segment) => Boolean(segment))
        .join('/') + '/'
    )
  }, [uns])

  const sunburstData = useMemo(() => {
    if (isLoading) return stratifyTopicTree([{ label: 'root', count: 1 }])

    const edgeTopics = toTreeMetadata(topics, unsPrefix)
    const remoteTopics = toTreeMetadata(topicSamples || [])
    return stratifyTopicTree([...edgeTopics, ...remoteTopics])
  }, [isLoading, topicSamples, topics, unsPrefix])

  if (isLoading) return <LoaderSpinner />

  const onHandleSelect = (topic: string, event: MouseEvent) => {
    onSelect?.(topic)
    event.stopPropagation()
  }

  return (
    <Card size="sm">
      <CardHeader as={HStack} gap={10}>
        <FormControl display="flex" data-testid="form-control-uns">
          <FormLabel htmlFor="switch-uns" mb={0}>
            {t('workspace.topicWheel.control.uns')}
          </FormLabel>
          <Switch
            id="switch-uns"
            isChecked={uns?.enabled}
            onChange={() => {
              if (uns) mutateAsync({ requestBody: { ...uns, enabled: !uns.enabled } })
            }}
          />
        </FormControl>
        <FormControl display="flex" alignItems="center" data-testid="form-control-origin">
          <FormLabel htmlFor="switch-origin" mb={0}>
            {t('workspace.topicWheel.control.origin')}
          </FormLabel>
          <Switch
            id="switch-origin"
            isChecked={useOrigin}
            onChange={() => {
              setUseOrigin((e) => !e)
            }}
          />
        </FormControl>
      </CardHeader>
      <CardBody onClick={() => onSelect?.(undefined)}>
        <Tabs variant="enclosed">
          <TabList>
            <Tab>{t('workspace.topicWheel.control.sunburst')}</Tab>
            <Tab>{t('workspace.topicWheel.control.treeview')}</Tab>
          </TabList>

          <TabPanels>
            <TabPanel w="100%" h="400px">
              <SunburstChart data={sunburstData} onSelect={onHandleSelect} />
            </TabPanel>
            <TabPanel>
              <TreeViewChart data={sunburstData} onSelect={onSelect} />
            </TabPanel>
          </TabPanels>
        </Tabs>
      </CardBody>
    </Card>
  )
}

export default TopicExplorer
