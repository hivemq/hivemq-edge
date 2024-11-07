import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'

import { useGetTopicSchemas } from '@/api/hooks/useDomainModel/useGetTopicSchemas.ts'
import { TagSchema, TopicFilter } from '@/api/__generated__'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import ErrorMessage from '@/components/ErrorMessage.tsx'
import JsonSchemaBrowser from '@/components/rjsf/MqttTransformation/JsonSchemaBrowser.tsx'

interface SchemaManagerProps {
  topicFilter: TopicFilter
}

const SchemaManager: FC<SchemaManagerProps> = ({ topicFilter }) => {
  const { t } = useTranslation()
  const { data, isLoading, isError, error } = useGetTopicSchemas([topicFilter.topicFilter as string])

  const isSchemaValid = useMemo(() => {
    return data && Object.keys(data).length !== 0 && data.constructor === Object
  }, [data])

  if (isLoading) return <LoaderSpinner />
  if (isError && error) return <ErrorMessage message={error.message} />
  if (!isSchemaValid) return <ErrorMessage message={t('topicFilter.error.noSchemaSampled')} />

  return <JsonSchemaBrowser schema={data as TagSchema} />
}

export default SchemaManager
