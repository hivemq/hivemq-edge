import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7 } from 'json-schema'
import { Button, Card, CardBody, CardFooter } from '@chakra-ui/react'

import { TopicFilter } from '@/api/__generated__'
import { useSamplingForTopic } from '@/api/hooks/useDomainModel/useSamplingForTopic.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { encodeDataUriJsonSchema } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'

interface SchemaManagerProps {
  topicFilter: TopicFilter
  onUpload: (s: string) => void
}

const SchemaSampler: FC<SchemaManagerProps> = ({ topicFilter, onUpload }) => {
  const { t } = useTranslation()
  const { schema, isLoading, isError, error } = useSamplingForTopic(topicFilter.topicFilter)

  const isSchemaValid = useMemo(() => {
    return schema && Object.keys(schema).length !== 0 && schema.constructor === Object
  }, [schema])

  if (isLoading) return <LoaderSpinner />
  if (isError && error) return <ErrorMessage message={error.message} />
  if (!isSchemaValid) return <ErrorMessage message={t('topicFilter.error.noSchemaSampled')} />

  return (
    <Card>
      <CardBody>
        <JsonSchemaBrowser schema={schema as JSONSchema7} />
      </CardBody>
      <CardFooter justifyContent="flex-end">
        <Button
          data-testid="schema-sampler-upload"
          onClick={() => onUpload(encodeDataUriJsonSchema(schema as JSONSchema7))}
        >
          {t('topicFilter.schema.actions.assign')}
        </Button>
      </CardFooter>
    </Card>
  )
}

export default SchemaSampler
