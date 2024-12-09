import { FC, useMemo } from 'react'
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

import { TopicFilter } from '@/api/__generated__'

import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader.tsx'
import SchemaManager from '@/modules/TopicFilters/components/SchemaManager.tsx'
import { useTopicFilterManager } from '@/modules/TopicFilters/hooks/useTopicFilterManager.ts'
import { SchemaHandler, validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'
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

  return (
    <VStack>
      <ErrorMessage message={schemaHandler.error} status={schemaHandler.status} type={schemaHandler.message} />

      <Card size="sm">
        <CardHeader>
          <Text>{t('topicFilter.schema.prompt')}</Text>
        </CardHeader>
        <CardBody>
          <Tabs isLazy>
            <TabList>
              <Tab>{t('topicFilter.schema.tabs.current')}</Tab>
              <Tab>{t('topicFilter.schema.tabs.upload')}</Tab>
              <Tab>{t('topicFilter.schema.tabs.infer')}</Tab>
            </TabList>

            <TabPanels>
              <TabPanel>
                {schemaHandler.schema && (
                  <Card>
                    <CardBody>
                      <JsonSchemaBrowser schema={schemaHandler.schema} />
                    </CardBody>
                    <CardFooter justifyContent="flex-end">
                      <Button isDisabled>{t('topicFilter.schema.actions.remove')}</Button>
                    </CardFooter>
                  </Card>
                )}
              </TabPanel>
              <TabPanel>
                <Card>
                  <SchemaUploader onUpload={onHandleUpload} />
                </Card>
              </TabPanel>
              <TabPanel>
                <Card>
                  <CardBody>
                    <SchemaManager topicFilter={topicFilter} />
                  </CardBody>
                  <CardFooter justifyContent="flex-end">
                    <Button isDisabled>{t('topicFilter.schema.actions.assign')}</Button>
                  </CardFooter>
                </Card>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </CardBody>
      </Card>
    </VStack>
  )
}

export default TopicSchemaManager
