import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7 } from 'json-schema'

import { TopicFilter } from '@/api/__generated__'
import { useSamplingForTopic } from '@/api/hooks/useDomainModel/useSamplingForTopic.ts'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'

interface SchemaManagerProps {
  topicFilter: TopicFilter
}

const SchemaManager: FC<SchemaManagerProps> = ({ topicFilter }) => {
  const { t } = useTranslation()
  const { schema, isLoading, isError, error } = useSamplingForTopic(topicFilter.topicFilter)

  const isSchemaValid = useMemo(() => {
    return schema && Object.keys(schema).length !== 0 && schema.constructor === Object
  }, [schema])

  if (isLoading) return <LoaderSpinner />
  if (isError && error) return <ErrorMessage message={error.message} />
  if (!isSchemaValid) return <ErrorMessage message={t('topicFilter.error.noSchemaSampled')} />

  return <JsonSchemaBrowser schema={schema as JSONSchema7} />
}

export default SchemaManager
