import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7 } from 'json-schema'
import type { CardProps } from '@chakra-ui/react'
import { Alert, AlertDescription, AlertIcon, Card, CardBody, CardHeader, Heading } from '@chakra-ui/react'

import { useListTopicFilters } from '@/api/hooks/useTopicFilters/useListTopicFilters.ts'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'
import type { JsonNode } from '@/api/__generated__'
import { DataIdentifierReference } from '@/api/__generated__'

interface DataModelSourcesProps extends Omit<CardProps, 'onChange'> {
  topic: string
  onChange?: (v: JsonNode | undefined) => void
}

const DataModelSources: FC<DataModelSourcesProps> = ({ topic, onChange, ...props }) => {
  const { t } = useTranslation()
  const { data: topicFilters, isLoading } = useListTopicFilters()

  const schemaResult = useMemo(() => {
    const topicFilter = topicFilters?.items?.find((tf) => tf.topicFilter === topic)
    return validateSchemaFromDataURI(topicFilter?.schema, DataIdentifierReference.type.TOPIC_FILTER)
  }, [topicFilters, topic])

  const structuredSchema = useMemo(() => {
    if (!schemaResult.schema) return [] as JSONSchema7[]
    return [{ ...schemaResult.schema, title: topic }] as JSONSchema7[]
  }, [schemaResult.schema, topic])

  useEffect(() => {
    onChange?.(schemaResult.schema as JsonNode | undefined)
  }, [onChange, schemaResult.schema])

  return (
    <Card {...props} size="sm">
      <CardHeader>
        <Heading as="h3" size="sm">
          {t('components:rjsf.MqttTransformationField.sources.header')}
        </Heading>
      </CardHeader>

      <CardBody maxH="55vh" overflowY="scroll" tabIndex={0}>
        {isLoading && <LoaderSpinner />}
        {!isLoading && schemaResult.status !== 'success' && (
          <Alert status={schemaResult.status}>
            <AlertIcon />
            <AlertDescription>{schemaResult.message}</AlertDescription>
          </Alert>
        )}
        {!isLoading &&
          schemaResult.status === 'success' &&
          structuredSchema.map((schema) => (
            <JsonSchemaBrowser schema={schema} isDraggable hasExamples showReadOnly={false} key={schema.title} />
          ))}
      </CardBody>
    </Card>
  )
}

export default DataModelSources
