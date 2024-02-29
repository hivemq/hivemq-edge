import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Tab, TabList, TabPanel, TabPanels, Tabs, Text } from '@chakra-ui/react'
import PolicyTable from '@datahub/components/pages/PolicyTable.tsx'
import SchemaTable from '@datahub/components/pages/SchemaTable.tsx'
import ScriptTable from '@datahub/components/pages/ScriptTable.tsx'

const DataHubListings: FC = () => {
  const { t } = useTranslation('datahub')

  return (
    <Tabs isLazy colorScheme="brand" data-testid="list-tabs">
      <TabList>
        <Tab fontSize="lg" fontWeight="bold">
          {t('Listings.tabs.policy.title')}
        </Tab>
        <Tab fontSize="lg" fontWeight="bold">
          {t('Listings.tabs.schema.title')}
        </Tab>
        <Tab fontSize="lg" fontWeight="bold">
          {t('Listings.tabs.script.title')}
        </Tab>
      </TabList>

      <TabPanels>
        <TabPanel>
          <Text mb={3}>{t('Listings.tabs.policy.description')}</Text>
          <PolicyTable />
        </TabPanel>
        <TabPanel>
          <Text mb={3}>{t('Listings.tabs.schema.description')}</Text>
          <SchemaTable />
        </TabPanel>
        <TabPanel>
          <Text mb={3}>{t('Listings.tabs.script.description')}</Text>
          <ScriptTable />
        </TabPanel>
      </TabPanels>
    </Tabs>
  )
}

export default DataHubListings
