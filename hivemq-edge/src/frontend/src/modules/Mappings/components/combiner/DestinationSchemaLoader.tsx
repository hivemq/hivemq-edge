import { type FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7 } from 'json-schema'

import {
  Button,
  ButtonGroup,
  Popover,
  PopoverArrow,
  PopoverBody,
  PopoverCloseButton,
  PopoverContent,
  PopoverFooter,
  PopoverHeader,
  PopoverTrigger,
  Text,
  VStack,
} from '@chakra-ui/react'

import type { DataCombining } from '@/api/__generated__'
import ErrorMessage from '@/components/ErrorMessage'
import SchemaUploader from '@/modules/TopicFilters/components/SchemaUploader'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser'
import { downloadJSON } from '@/extensions/datahub/utils/download.utils'

interface DestinationSchemaLoaderProps {
  formData?: DataCombining
  onChange: (schema: string) => void
}

export const DestinationSchemaLoader: FC<DestinationSchemaLoaderProps> = ({ formData, onChange }) => {
  const { t } = useTranslation()
  const isTopicDefined = Boolean(formData?.destination?.topic && formData?.destination?.topic !== '')
  const isSchemaDefined = Boolean(formData?.destination?.schema && formData?.destination?.schema !== '')

  const handleSchemaUpload = (schema: string) => {
    onChange(schema)
  }

  const handleSchemaDownload = () => {
    if (!formData?.destination?.schema) return

    const handler = validateSchemaFromDataURI(formData?.destination?.schema)
    if (handler.schema) downloadJSON<JSONSchema7>(handler.schema.title || 'topic-untitled', handler.schema)
  }

  const schema = useMemo(() => {
    if (!formData?.destination?.schema) return undefined
    return validateSchemaFromDataURI(formData?.destination?.schema)
  }, [formData?.destination?.schema])

  return (
    <>
      <ButtonGroup size="sm" variant="outline" justifyContent={'flex-end'}>
        <Popover>
          <PopoverTrigger>
            <Button isDisabled>{t('combiner.schema.schemaManager.action.infer')}</Button>
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
            <Button isDisabled={!isTopicDefined}>{t('combiner.schema.schemaManager.action.upload')}</Button>
          </PopoverTrigger>
          <PopoverContent>
            <PopoverArrow />
            <PopoverCloseButton />
            <PopoverHeader>{t('combiner.schema.schemaManager.header')}</PopoverHeader>
            <PopoverBody>
              <SchemaUploader onUpload={handleSchemaUpload} />
            </PopoverBody>
          </PopoverContent>
        </Popover>
        <Button onClick={handleSchemaDownload} isDisabled={!isSchemaDefined}>
          {t('combiner.schema.schemaManager.action.download')}
        </Button>
      </ButtonGroup>

      {!formData?.destination?.schema && (
        <VStack w="100%" height={420} justifyContent={'center'} alignItems={'center'}>
          <ErrorMessage message={t('combiner.error.noSchemaLoadedYet')} status={'info'} />
        </VStack>
      )}

      {schema?.schema && (
        <VStack w="100%" height={420} justifyContent={'center'} alignItems={'flex-start'}>
          <JsonSchemaBrowser schema={schema.schema} isDraggable hasExamples />
        </VStack>
        // <VStack w="100%" height={420} justifyContent={'center'} alignItems={'stretch'} gap={3}>
        //   <MappingInstructionList
        //     schema={schema.schema}
        //     instructions={[]}
        //     display={'flex'}
        //     flexDirection={'column'}
        //     gap={4}
        //   />
        // </VStack>
      )}
    </>
  )
}
