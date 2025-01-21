import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Tab, TabList, TabPanel, TabPanels, type TabPanelProps, Tabs, VStack } from '@chakra-ui/react'

import TopicWheel from '@/modules/DomainOntology/components/TopicWheel.tsx'
import { HierarchicalEdgeBundling } from '@/modules/DomainOntology/components/charts/HierarchicalEdgeBundling.tsx'
import { useGetDomainOntology } from '@/modules/DomainOntology/hooks/useGetDomainOntology.ts'
import ChordChart from '@/modules/DomainOntology/components/charts/ChordChart.tsx'
import SankeyChart from '@/modules/DomainOntology/components/charts/SankeyChart.tsx'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import AdapterCluster from '@/modules/DomainOntology/components/AdapterCluster.tsx'

const DomainOntologyManager: FC = () => {
  const { t } = useTranslation()
  const { treeData, sunburstData, matrixData, sankeyData } = useGetDomainOntology()

  const panelProps: TabPanelProps = { px: 0, as: VStack, alignItems: 'stretch' }

  return (
    <Tabs variant="soft-rounded" isLazy>
      <TabList>
        <Tab>{t('ontology.panel.cluster')}</Tab>
        <Tab>{t('ontology.panel.wheel')}</Tab>
        <Tab>{t('ontology.panel.chord')}</Tab>
        <Tab>{t('ontology.panel.sankey')}</Tab>
        <Tab>{t('ontology.panel.edge-blunting')}</Tab>
      </TabList>
      <TabPanels>
        <TabPanel {...panelProps}>
          <AdapterCluster />
        </TabPanel>
        <TabPanel {...panelProps}>
          <TopicWheel data={sunburstData} />
        </TabPanel>
        <TabPanel {...panelProps}>
          <ChartWrapper>
            <ChordChart matrix={matrixData.matrix} keys={matrixData.keys}></ChordChart>
          </ChartWrapper>
        </TabPanel>
        <TabPanel {...panelProps}>
          <ChartWrapper>
            <SankeyChart data={sankeyData}></SankeyChart>
          </ChartWrapper>
        </TabPanel>
        <TabPanel {...panelProps}>
          <ChartWrapper>
            <HierarchicalEdgeBundling data={treeData} width={800} height={800} />
          </ChartWrapper>
        </TabPanel>
      </TabPanels>
    </Tabs>
  )
}

export default DomainOntologyManager
