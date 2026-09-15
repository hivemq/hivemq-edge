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
 * The northbound document wraps the value in a fixed envelope (tagName, timestamp, metadata) while the
 * southbound document is value-only, so the two documents as wholes are never equal. "The same schema" to
 * someone looking at this panel means the value shape, so that is what is compared.
 */
const valueShape = (schema: JsonNode): JsonNode => ((schema as JSONSchema7).properties?.value as JsonNode) ?? schema

/**
 * Structural equality: object member order carries no meaning in JSON, and the two directions are assembled
 * independently, so `{eventId, method}` and `{method, eventId}` are the same shape and must compare equal.
 * Array order *is* significant — `required`, `enum` and `anyOf` are sequences, not sets, as far as this
 * panel's display is concerned.
 *
 * Compared structurally rather than by sorting keys and stringifying: sorting would need a comparator, and a
 * locale-aware one would make the result depend on the machine's locale, which is the opposite of what a
 * canonical form is for.
 */
const isSameStructure = (a: unknown, b: unknown): boolean => {
  if (a === b) return true
  if (Array.isArray(a) || Array.isArray(b)) {
    if (!Array.isArray(a) || !Array.isArray(b) || a.length !== b.length) return false
    return a.every((item, index) => isSameStructure(item, b[index]))
  }
  if (a === null || b === null || typeof a !== 'object' || typeof b !== 'object') return false

  const left = a as Record<string, unknown>
  const right = b as Record<string, unknown>
  const leftKeys = Object.keys(left)
  if (leftKeys.length !== Object.keys(right).length) return false
  return leftKeys.every((key) => Object.hasOwn(right, key) && isSameStructure(left[key], right[key]))
}

const isSameShape = (a: JsonNode, b: JsonNode): boolean => isSameStructure(valueShape(a), valueShape(b))

export const TagSchemaPanel: FC<TagSchemaPanelProps> = ({ tag, adapterId }) => {
  // This panel is pure inspection, so it shows both directions. They are usually the same for a plain value
  // tag; they differ when a tag's southbound shape is not a projection of its northbound shape (e.g. a
  // condition tag).
  const {
    data: northboundSchema,
    isLoading: isLoadingNorthbound,
    isError: isErrorNorthbound,
  } = useGetSchema(adapterId, tag.name)
  const {
    data: southboundSchema,
    isLoading: isLoadingSouthbound,
    isError: isErrorSouthbound,
  } = useGetSchema(adapterId, tag.name, 'SOUTHBOUND')
  const { t } = useTranslation()

  const areIdentical = useMemo(
    () => Boolean(northboundSchema && southboundSchema && isSameShape(northboundSchema, southboundSchema)),
    [northboundSchema, southboundSchema]
  )

  const handleSchemaDownload = () => {
    if (!northboundSchema) return

    // TODO[NVL] This should be transformed into an async method (react-query type) with error management and testing
    downloadJSON<JSONSchema7>(northboundSchema.title || 'topic-untitled', northboundSchema)
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
        {/* The two queries are independent: the northbound panel renders as soon as its own request settles. */}
        {isLoadingNorthbound && <LoaderSpinner />}
        {isErrorNorthbound && <ErrorMessage message={t('device.errors.noNorthboundSchemaLoaded')} />}
        {northboundSchema && (
          <FormControl data-testid="tag-schema-panel" id="tag-schema-panel">
            <FormLabel>
              {areIdentical ? t('device.drawer.schema.label') : t('device.drawer.schema.labelNorthbound')}
            </FormLabel>
            <Box borderWidth={1} p={2} data-testid="tag-schema-northbound">
              <JsonSchemaBrowser schema={northboundSchema} hasExamples />
            </Box>
            <FormHelperText>
              {areIdentical ? t('device.drawer.schema.identical') : t('device.drawer.schema.helperNorthbound')}
            </FormHelperText>
          </FormControl>
        )}

        {isLoadingSouthbound && !isLoadingNorthbound && <LoaderSpinner />}
        {isErrorSouthbound && <ErrorMessage message={t('device.errors.noSouthboundSchemaLoaded')} />}
        {!areIdentical && southboundSchema && (
          <FormControl mt={4} data-testid="tag-schema-panel-southbound" id="tag-schema-panel-southbound">
            <FormLabel>{t('device.drawer.schema.labelSouthbound')}</FormLabel>
            <Box borderWidth={1} p={2} data-testid="tag-schema-southbound">
              <JsonSchemaBrowser schema={southboundSchema} hasExamples />
            </Box>
            <FormHelperText>{t('device.drawer.schema.helperSouthbound')}</FormHelperText>
          </FormControl>
        )}
      </CardBody>
      {northboundSchema && (
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
