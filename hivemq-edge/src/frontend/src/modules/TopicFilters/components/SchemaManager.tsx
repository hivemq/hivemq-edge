import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7 } from 'json-schema'

import { TopicFilter } from '@/api/__generated__'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'
import { useGetTopicSchema } from '@/api/hooks/_deprecated/useGetTagSchema.ts'

interface SchemaManagerProps {
  topicFilter: TopicFilter
}

const SchemaManager: FC<SchemaManagerProps> = ({ topicFilter }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetTopicSchema(topicFilter.topicFilter)

  const isSchemaValid = useMemo(() => {
    return data && Object.keys(data).length !== 0 && data.constructor === Object
  }, [data])

  if (isLoading) return <LoaderSpinner />
  if (isError && error) return <ErrorMessage message={error.message} />
  if (!isSchemaValid) return <ErrorMessage message={t('topicFilter.error.noSchemaSampled')} />

  return <JsonSchemaBrowser schema={data?.configSchema as JSONSchema7} />
}

export default SchemaManager
