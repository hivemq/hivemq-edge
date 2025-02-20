import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import type { FieldProps, FormContextType, RJSFSchema } from '@rjsf/utils'
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
  VStack,
} from '@chakra-ui/react'
import { FaRightFromBracket } from 'react-icons/fa6'

import type { DataCombining } from '@/api/__generated__'
import { SelectTopic } from '@/components/MQTT/EntityCreatableSelect'
import ErrorMessage from '@/components/ErrorMessage'
import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader'
import CombinedEntitySelect from './CombinedEntitySelect'

const DataCombiningEditorField: FC<FieldProps<DataCombining, RJSFSchema, FormContextType>> = (props) => {
  const { t } = useTranslation()

  const { formData } = props

  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={1} alignItems="stretch" maxW="40vw">
          <Box>
            <CombinedEntitySelect tags={formData?.sources?.tags} topicFilters={formData?.sources?.topicFilters} />
          </Box>
          <VStack minH={250} justifyContent={'center'}>
            <ErrorMessage message={'There are no schema available yet'} status={'info'} />
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
                <PopoverHeader>Confirmation!</PopoverHeader>
                <PopoverBody>We will create a new schema based on the combination of the sources</PopoverBody>
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
