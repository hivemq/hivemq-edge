import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Tab, TabList, TabPanel, TabPanels, Tabs, VStack } from '@chakra-ui/react'

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

  return (
    <Tabs variant="enclosed" isLazy>
      <TabList>
        <Tab>{t('ontology.panel.cluster')}</Tab>
        <Tab>{t('ontology.panel.wheel')}</Tab>
        <Tab>{t('ontology.panel.chord')}</Tab>
        <Tab>{t('ontology.panel.sankey')}</Tab>
        <Tab>{t('ontology.panel.edge-blunting')}</Tab>
      </TabList>
      <TabPanels>
        <TabPanel px={0} as={VStack} alignItems="stretch">
          <AdapterCluster />
        </TabPanel>
        <TabPanel px={0} as={VStack} alignItems="stretch">
          <TopicWheel data={sunburstData} />
        </TabPanel>
        <TabPanel px={0} as={VStack} alignItems="stretch">
          <ChartWrapper>
            <Box w="100%" h="69vh">
              <ChordChart matrix={matrixData.matrix} keys={matrixData.keys}></ChordChart>
            </Box>
          </ChartWrapper>
        </TabPanel>
        <TabPanel px={0} as={VStack} alignItems="stretch">
          <ChartWrapper>
            <Box w="100%" h="69vh">
              <SankeyChart data={sankeyData}></SankeyChart>
            </Box>
          </ChartWrapper>
        </TabPanel>
        <TabPanel px={0} as={VStack} alignItems="stretch">
          <ChartWrapper>
            <Box w="100%" h="100%">
              <HierarchicalEdgeBundling data={treeData} width={800} height={800} />
            </Box>
          </ChartWrapper>
        </TabPanel>
      </TabPanels>
    </Tabs>
  )
}

export default DomainOntologyManager
