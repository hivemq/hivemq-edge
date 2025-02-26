import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Select } from 'chakra-react-select'
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

import { DataCombining } from '@/api/__generated__'
import { SelectTopic } from '@/components/MQTT/EntityCreatableSelect'
import ErrorMessage from '@/components/ErrorMessage'
import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader'
import type { CombinerContext } from '@/modules/Mappings/types'
import CombinedEntitySelect from './CombinedEntitySelect'
import { CombinedSchemaLoader } from './CombinedSchemaLoader'

export const DataCombiningEditorField: FC<FieldProps<DataCombining, RJSFSchema, CombinerContext>> = (props) => {
  const { t } = useTranslation()

  const { formData, formContext } = props

  const primary = useMemo(() => {
    const tags = formData?.sources?.tags || []
    const topicFilters = formData?.sources?.topicFilters || []

    return [...tags, ...topicFilters].map((entity) => ({ label: entity }))
  }, [formData])

  return (
    <VStack alignItems="stretch" gap={4}>
      <Stack gap={2} flexDirection="row">
        <VStack flex={1} alignItems="stretch" maxW="40vw">
          <Box>
            <CombinedEntitySelect
              tags={formData?.sources?.tags}
              topicFilters={formData?.sources?.topicFilters}
              formContext={formContext}
              onChange={(values) => {
                if (!formData) return
                const tag: string[] = []
                const filter: string[] = []
                values.forEach((entity) => {
                  if (entity.type === DataCombining.primaryType.TAG) tag.push(entity.value)
                  if (entity.type === DataCombining.primaryType.TOPIC_FILTER) filter.push(entity.value)
                })

                props.onChange({
                  ...formData,
                  // @ts-ignore TODO check for type clash on primary
                  sources: { ...formData.sources, tags: tag, topicFilters: filter },
                })
              }}
            />
          </Box>
          <VStack height={500} overflow={'auto'} alignItems={'flex-start'} justifyContent={'center'} tabIndex={0}>
            <CombinedSchemaLoader formData={props.formData} formContext={formContext} />
          </VStack>
          <Box>
            <Select<{ label: string }>
              options={primary}
              data-testid={'combiner-mapping-primary'}
              value={undefined}
              isClearable
              placeholder={t('combiner.schema.mapping.primary.placeholder')}
            />
          </Box>
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
              id={'destination'}
              value={formData?.destination?.topic || null}
              onChange={(topic) => {
                if (!props.formData) return

                if (topic && typeof topic === 'string')
                  props.onChange({ ...props.formData, destination: { topic: topic } })
                else if (!topic) props.onChange({ ...props.formData, destination: { topic: '' } })
              }}
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
                <PopoverHeader>{t('combiner.schema.schemaManager.header')}</PopoverHeader>
                <PopoverBody>
                  <Text>{t('combiner.schema.schemaManager.infer.message')}</Text>
                </PopoverBody>
                <PopoverFooter gap={2} display={'flex'}>
                  <Button isDisabled>{t('combiner.schema.schemaManager.infer.action')}</Button>
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
                <PopoverHeader>{t('combiner.schema.schemaManager.header')}</PopoverHeader>
                <PopoverBody>
                  <SchemaUploader onUpload={() => console.log('uploaded')} />
                </PopoverBody>
              </PopoverContent>
            </Popover>
          </ButtonGroup>
          <VStack height={420} justifyContent={'center'} alignItems={'center'}>
            <ErrorMessage message={t('combiner.error.noSchemaLoadedYet')} status={'info'} />
          </VStack>
        </VStack>
      </Stack>
    </VStack>
  )
}
