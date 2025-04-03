import type { JSONSchema7 } from 'json-schema'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Box,
  ButtonGroup,
  CardFooter,
  FormControl,
  FormHelperText,
  FormLabel,
  Button,
  Card,
  CardBody,
  CardHeader,
  Text,
} from '@chakra-ui/react'

import type { DomainTag } from '@/api/__generated__'
import { useGetWritingSchema } from '@/api/hooks/useProtocolAdapters/useGetWritingSchema'
import { PLCTag } from '@/components/MQTT/EntityTag.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner'
import ErrorMessage from '@/components/ErrorMessage'
import { downloadJSON } from '@/utils/download.utils'

interface TagSchemaPanelProps {
  tag: DomainTag
  adapterId: string
}

export const TagSchemaPanel: FC<TagSchemaPanelProps> = ({ tag, adapterId }) => {
  const { data, isLoading, isError } = useGetWritingSchema(adapterId, tag.name)
  const { t } = useTranslation()

  const handleSchemaDownload = () => {
    if (!data) return

    // TODO[NVL] This should be transformed into an async method (react-query type) with error management and testing
    downloadJSON<JSONSchema7>(data.title || 'topic-untitled', data)
  }

  return (
    <Card size="sm">
      <CardHeader>
        <Text as="span" data-testid={'tag-schema-header'}>
          {t('device.drawer.table.column.name')}
        </Text>{' '}
        <PLCTag tagTitle={tag.name} mr={3} />
      </CardHeader>
      <CardBody>
        {isLoading && <LoaderSpinner />}

        {isError && <ErrorMessage message={t('device.errors.noSchemaLoaded')} />}
        {data && (
          <FormControl isInvalid={isError} data-testid={'tag-schema-panel'} id={'tag-schema-panel'}>
            <FormLabel>{t('device.drawer.schema.label')}</FormLabel>
            <Box borderWidth={1} p={2}>
              <JsonSchemaBrowser schema={data} hasExamples />
            </Box>
            <FormHelperText>{t('device.drawer.schema.helper')}</FormHelperText>
          </FormControl>
        )}
      </CardBody>
      {data && (
        <CardFooter>
          <ButtonGroup>
            <Button data-testid={'tag-schema-download'} onClick={handleSchemaDownload}>
              {t('device.drawer.schema.action.download')}
            </Button>
          </ButtonGroup>
        </CardFooter>
      )}
    </Card>
  )
}
