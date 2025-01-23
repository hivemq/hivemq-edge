import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Tab, TabList, TabPanel, TabPanels, type TabPanelProps, Tabs, VStack } from '@chakra-ui/react'

import ConceptWheel from '@/modules/DomainOntology/components/ConceptWheel.tsx'

import AdapterCluster from '@/modules/DomainOntology/components/AdapterCluster.tsx'
import RelationMatrix from '@/modules/DomainOntology/components/RelationMatrix.tsx'
import ConceptFlow from '@/modules/DomainOntology/components/ConceptFlow.tsx'
import RelationEdgeBundling from '@/modules/DomainOntology/components/RelationEdgeBundling.tsx'
import config from '@/config'

const DomainOntologyManager: FC = () => {
  const { t } = useTranslation()

  const panelProps: TabPanelProps = { px: 0, as: VStack, alignItems: 'stretch' }

  return (
    <Tabs variant="solid-rounded" isLazy>
      <TabList>
        <Tab>{t('ontology.panel.cluster')}</Tab>
        <Tab>{t('ontology.panel.wheel')}</Tab>
        <Tab>{t('ontology.panel.chord')}</Tab>
        <Tab>{t('ontology.panel.sankey')}</Tab>
        {config.features.WORKSPACE_EXPERIMENTAL && <Tab>{t('ontology.panel.edge-blunting')}</Tab>}
      </TabList>
      <TabPanels>
        <TabPanel {...panelProps}>
          <AdapterCluster />
        </TabPanel>
        <TabPanel {...panelProps}>
          <ConceptWheel />
        </TabPanel>
        <TabPanel {...panelProps}>
          <RelationMatrix />
        </TabPanel>
        <TabPanel {...panelProps}>
          <ConceptFlow />
        </TabPanel>
        {config.features.WORKSPACE_EXPERIMENTAL && (
          <TabPanel {...panelProps}>
            <RelationEdgeBundling />
          </TabPanel>
        )}
      </TabPanels>
    </Tabs>
  )
}

export default DomainOntologyManager
