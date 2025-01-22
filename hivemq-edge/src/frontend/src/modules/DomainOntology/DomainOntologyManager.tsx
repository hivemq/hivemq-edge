import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Tab, TabList, TabPanel, TabPanels, type TabPanelProps, Tabs, VStack } from '@chakra-ui/react'

import ConceptWheel from '@/modules/DomainOntology/components/ConceptWheel.tsx'

import AdapterCluster from '@/modules/DomainOntology/components/AdapterCluster.tsx'
import RelationshipMatrix from '@/modules/DomainOntology/components/RelationshipMatrix.tsx'
import ConceptFlow from '@/modules/DomainOntology/components/ConceptFlow.tsx'
import RelationshipEdgeBundling from '@/modules/DomainOntology/components/RelationshipEdgeBundling.tsx'

const DomainOntologyManager: FC = () => {
  const { t } = useTranslation()

  const panelProps: TabPanelProps = { px: 0, as: VStack, alignItems: 'stretch' }

  return (
    <Tabs variant="soft-rounded" isLazy>
      <TabList>
        <Tab>{t('ontology.panel.cluster')}</Tab>
        <Tab>{t('ontology.panel.wheel')}</Tab>
        <Tab>{t('ontology.panel.chord')}</Tab>
        <Tab>{t('ontology.panel.sankey')}</Tab>
        {/*<Tab>{t('ontology.panel.edge-blunting')}</Tab>*/}
      </TabList>
      <TabPanels>
        <TabPanel {...panelProps}>
          <AdapterCluster />
        </TabPanel>
        <TabPanel {...panelProps}>
          <ConceptWheel />
        </TabPanel>
        <TabPanel {...panelProps}>
          <RelationshipMatrix />
        </TabPanel>
        <TabPanel {...panelProps}>
          <ConceptFlow />
        </TabPanel>
        <TabPanel {...panelProps}>
          <RelationshipEdgeBundling />
        </TabPanel>
      </TabPanels>
    </Tabs>
  )
}

export default DomainOntologyManager
