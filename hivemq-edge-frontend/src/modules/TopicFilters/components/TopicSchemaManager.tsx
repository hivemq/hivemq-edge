import type { FC } from 'react'
import { useMemo } from 'react'
import {
  Button,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Text,
  VStack,
} from '@chakra-ui/react'

import type { TopicFilter } from '@/api/__generated__'

import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader.tsx'
import SchemaSampler from '@/modules/TopicFilters/components/SchemaSampler.tsx'
import { useTopicFilterManager } from '@/modules/TopicFilters/hooks/useTopicFilterManager.ts'
import type { SchemaHandler } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'
import { useTranslation } from 'react-i18next'

interface CurrentSchemaProps {
  topicFilter: TopicFilter
}

const TopicSchemaManager: FC<CurrentSchemaProps> = ({ topicFilter }) => {
  const { t } = useTranslation()
  const schemaHandler = useMemo<SchemaHandler>(
    () => validateSchemaFromDataURI(topicFilter.schema),
    [topicFilter.schema]
  )
  const { onUpdate } = useTopicFilterManager()

  const onHandleUpload = (dataUri: string) => {
    onUpdate(topicFilter.topicFilter, { ...topicFilter, schema: dataUri })
  }

  const onHandleClear = () => {
    onUpdate(topicFilter.topicFilter, { ...topicFilter, schema: undefined })
  }

  return (
    <VStack>
      <ErrorMessage status={schemaHandler.status} type={schemaHandler.message} />

      <Card size="sm">
        <CardHeader>
          <Text>{t('topicFilter.schema.prompt')}</Text>
        </CardHeader>
        <CardBody>
          <Tabs isLazy>
            <TabList>
              <Tab>{t('topicFilter.schema.tabs.current')}</Tab>
              <Tab>{t('topicFilter.schema.tabs.infer')}</Tab>
              <Tab>{t('topicFilter.schema.tabs.upload')}</Tab>
            </TabList>

            <TabPanels>
              <TabPanel>
                <Card>
                  <CardBody>
                    {schemaHandler.error && (
                      <ErrorMessage message={schemaHandler.error} status={schemaHandler.status} />
                    )}
                    {schemaHandler.schema && <JsonSchemaBrowser schema={schemaHandler.schema} hasExamples />}
                  </CardBody>
                  <CardFooter justifyContent="flex-end">
                    <Button isDisabled={Boolean(!topicFilter.schema)} onClick={onHandleClear}>
                      {t('topicFilter.schema.actions.remove')}
                    </Button>
                  </CardFooter>
                </Card>
              </TabPanel>
              <TabPanel>
                <SchemaSampler topicFilter={topicFilter} onUpload={onHandleUpload} />
              </TabPanel>
              <TabPanel>
                <SchemaUploader onUpload={onHandleUpload} />
              </TabPanel>
            </TabPanels>
          </Tabs>
        </CardBody>
      </Card>
    </VStack>
  )
}

export default TopicSchemaManager
