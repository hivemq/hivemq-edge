import type { JSONSchema7 } from 'json-schema'
import type { FC } from 'react'
import { useMemo } from 'react'
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

import type { DomainTag, JsonNode } from '@/api/__generated__'
import { useGetSchema } from '@/api/hooks/useProtocolAdapters/useGetSchema'
import { PLCTag } from '@/components/MQTT/EntityTag.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner'
import ErrorMessage from '@/components/ErrorMessage'
import { downloadJSON } from '@/utils/download.utils'

interface TagSchemaPanelProps {
  tag: DomainTag
  adapterId: string
}

/**
 * The read document wraps the value in a fixed envelope (tagName, timestamp, metadata) while the write
 * document is value-only, so the two documents as wholes are never equal. "The same schema" to someone
 * looking at this panel means the value shape, so that is what is compared.
 */
const valueShape = (schema: JsonNode): JsonNode => ((schema as JSONSchema7).properties?.value as JsonNode) ?? schema

/**
 * Object member order carries no meaning in JSON, and the two directions are assembled independently, so
 * `{eventId, method}` and `{method, eventId}` are the same shape and must compare equal. Array order is
 * preserved: `required`, `enum` and `anyOf` are sequences, not sets, as far as this panel's display is
 * concerned.
 */
const canonical = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(canonical)
  if (value === null || typeof value !== 'object') return value
  return Object.fromEntries(
    Object.keys(value as Record<string, unknown>)
      .sort()
      .map((key) => [key, canonical((value as Record<string, unknown>)[key])])
  )
}

const isSameShape = (a: JsonNode, b: JsonNode): boolean =>
  JSON.stringify(canonical(valueShape(a))) === JSON.stringify(canonical(valueShape(b)))

export const TagSchemaPanel: FC<TagSchemaPanelProps> = ({ tag, adapterId }) => {
  // This panel is pure inspection, so it shows both directions. They are usually the same for a plain value
  // tag; they differ when a tag's write shape is not a projection of its read shape (e.g. a condition tag).
  const { data: readSchema, isLoading: isLoadingRead, isError: isErrorRead } = useGetSchema(adapterId, tag.name)
  const {
    data: writeSchema,
    isLoading: isLoadingWrite,
    isError: isErrorWrite,
  } = useGetSchema(adapterId, tag.name, 'SOUTHBOUND')
  const { t } = useTranslation()

  const areIdentical = useMemo(
    () => Boolean(readSchema && writeSchema && isSameShape(readSchema, writeSchema)),
    [readSchema, writeSchema]
  )

  const handleSchemaDownload = () => {
    if (!readSchema) return

    // TODO[NVL] This should be transformed into an async method (react-query type) with error management and testing
    downloadJSON<JSONSchema7>(readSchema.title || 'topic-untitled', readSchema)
  }

  return (
    <Card size="sm">
      <CardHeader>
        <Text as="span" data-testid="tag-schema-header">
          {t('device.drawer.table.column.name')}
        </Text>{' '}
        <PLCTag tagTitle={tag.name} mr={3} />
      </CardHeader>
      <CardBody>
        {/* The two queries are independent: the read panel renders as soon as its own request settles. */}
        {isLoadingRead && <LoaderSpinner />}
        {isErrorRead && <ErrorMessage message={t('device.errors.noSchemaLoaded')} />}
        {readSchema && (
          <FormControl data-testid="tag-schema-panel" id="tag-schema-panel">
            <FormLabel>
              {areIdentical ? t('device.drawer.schema.label') : t('device.drawer.schema.labelRead')}
            </FormLabel>
            <Box borderWidth={1} p={2} data-testid="tag-schema-read">
              <JsonSchemaBrowser schema={readSchema} hasExamples />
            </Box>
            <FormHelperText>
              {areIdentical ? t('device.drawer.schema.identical') : t('device.drawer.schema.helperRead')}
            </FormHelperText>
          </FormControl>
        )}

        {isLoadingWrite && !isLoadingRead && <LoaderSpinner />}
        {isErrorWrite && <ErrorMessage message={t('device.errors.noWriteSchemaLoaded')} />}
        {!areIdentical && writeSchema && (
          <FormControl mt={4} data-testid="tag-schema-panel-write" id="tag-schema-panel-write">
            <FormLabel>{t('device.drawer.schema.labelWrite')}</FormLabel>
            <Box borderWidth={1} p={2} data-testid="tag-schema-write">
              <JsonSchemaBrowser schema={writeSchema} hasExamples />
            </Box>
            <FormHelperText>{t('device.drawer.schema.helperWrite')}</FormHelperText>
          </FormControl>
        )}
      </CardBody>
      {readSchema && (
        <CardFooter>
          <ButtonGroup>
            <Button data-testid="tag-schema-download" onClick={handleSchemaDownload}>
              {t('device.drawer.schema.action.download')}
            </Button>
          </ButtonGroup>
        </CardFooter>
      )}
    </Card>
  )
}
