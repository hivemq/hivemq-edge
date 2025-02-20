import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { FieldProps, RJSFSchema } from '@rjsf/utils'
import {
  Box,
  Button,
  ButtonGroup,
  HStack,
  Icon,
  Popover,
  PopoverTrigger,
  PopoverContent,
  PopoverHeader,
  PopoverBody,
  PopoverArrow,
  PopoverCloseButton,
  Stack,
  Text,
  VStack,
  PopoverFooter,
} from '@chakra-ui/react'
import { FaRightFromBracket } from 'react-icons/fa6'

import { MOCK_MQTT_SCHEMA_PLAIN, MOCK_MQTT_SCHEMA_REFS } from '@/__test-utils__/rjsf/schema.mocks'
import type { DataCombining } from '@/api/__generated__'
import { GENERATE_DATA_MODELS } from '@/api/hooks/useDomainModel/__handlers__'
import { SelectTopic } from '@/components/MQTT/EntityCreatableSelect'
import ErrorMessage from '@/components/ErrorMessage'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser'
import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader'
import CombinedEntitySelect from './CombinedEntitySelect'
import type { CombinerContext } from '../../types'

const DataCombiningEditorField: FC<FieldProps<DataCombining, RJSFSchema, CombinerContext>> = (props) => {
  const { t } = useTranslation()

  const { formData, formContext } = props

  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={1} alignItems="stretch" maxW="40vw">
          <Box>
            <CombinedEntitySelect
              tags={formData?.sources?.tags}
              topicFilters={formData?.sources?.topicFilters}
              options={formContext?.sources}
            />
          </Box>
          <VStack height={500} overflow={'auto'} alignItems={'flex-start'}>
            <JsonSchemaBrowser schema={{ ...MOCK_MQTT_SCHEMA_PLAIN, title: 'my/tag/t1' }} hasExamples />
            <JsonSchemaBrowser schema={{ ...MOCK_MQTT_SCHEMA_REFS, title: 'my/tag/t3' }} hasExamples />
            <JsonSchemaBrowser schema={{ ...GENERATE_DATA_MODELS(true), title: 'my/tag/t3' }} hasExamples />
          </VStack>
        </VStack>
        <VStack justifyContent="center">
          <HStack height={38}>
            <Icon as={FaRightFromBracket} />
          </HStack>
        </VStack>
        <VStack flex={1} alignItems="stretch" maxW="50vw">
          <Box>
            <SelectTopic
              isMulti={false}
              isCreatable={true}
              id={'id'}
              value={formData?.destination || null}
              onChange={(e) => console.log(e)}
            />
          </Box>
          <ButtonGroup size="sm" variant="outline" justifyContent={'flex-end'}>
            <Popover>
              <PopoverTrigger>
                <Button>{t('topicFilter.schema.tabs.infer')}</Button>
              </PopoverTrigger>
              <PopoverContent>
                <PopoverArrow />
                <PopoverCloseButton />
                <PopoverHeader>Create a schema</PopoverHeader>
                <PopoverBody>
                  <Text>We will create a new schema based on the combination of the sources</Text>
                </PopoverBody>
                <PopoverFooter gap={2} display={'flex'}>
                  <Button>Generate</Button>
                  <Button isDisabled>Download</Button>
                </PopoverFooter>
              </PopoverContent>
            </Popover>
            <Popover>
              <PopoverTrigger>
                <Button>{t('topicFilter.schema.tabs.upload')}</Button>
              </PopoverTrigger>
              <PopoverContent>
                <PopoverArrow />
                <PopoverCloseButton />
                <PopoverHeader>Confirmation!</PopoverHeader>
                <PopoverBody>
                  <SchemaUploader onUpload={() => console.log('XXXXX')} />
                </PopoverBody>
              </PopoverContent>
            </Popover>
          </ButtonGroup>
          <VStack minH={170} justifyContent={'center'}>
            <ErrorMessage message={'There are no schema available yet'} status={'info'} />
          </VStack>
        </VStack>
      </Stack>
    </VStack>
  )
}

export default DataCombiningEditorField
