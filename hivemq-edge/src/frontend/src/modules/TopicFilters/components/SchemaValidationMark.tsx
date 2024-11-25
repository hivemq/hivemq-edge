import { FC, useMemo } from 'react'
import { Alert, AlertIcon, Spinner } from '@chakra-ui/react'

import { TopicFilter } from '@/api/__generated__'
import { useGetTopicSchema } from '@/api/hooks/_deprecated/useGetTagSchema.ts'

interface SchemaValidationMarkProps {
  topicFilter: TopicFilter
}

const SchemaValidationMark: FC<SchemaValidationMarkProps> = ({ topicFilter }) => {
  const { data, isLoading } = useGetTopicSchema(topicFilter.topicFilter)

  const isSchemaValid = useMemo(() => {
    return data && Object.keys(data).length !== 0 && data.constructor === Object
  }, [data])

  if (isLoading) return <Spinner size="xs" data-testid="validation-loading" />
  return (
    <Alert status={isSchemaValid ? 'success' : 'error'} size="xs" py={1} pl={2}>
      <AlertIcon />
    </Alert>
  )
}

export default SchemaValidationMark
