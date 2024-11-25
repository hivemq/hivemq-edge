import { FC, useMemo } from 'react'
import { Alert, AlertIcon, Spinner } from '@chakra-ui/react'

import { TopicFilter } from '@/api/__generated__'
import { useSamplingForTopic } from '@/api/hooks/useDomainModel/useSamplingForTopic.ts'

interface SchemaValidationMarkProps {
  topicFilter: TopicFilter
}

const SchemaValidationMark: FC<SchemaValidationMarkProps> = ({ topicFilter }) => {
  const { schema, isLoading } = useSamplingForTopic(topicFilter.topicFilter)

  const isSchemaValid = useMemo(() => {
    return schema && Object.keys(schema).length !== 0 && schema.constructor === Object
  }, [schema])

  if (isLoading) return <Spinner size="xs" data-testid="validation-loading" />
  return (
    <Alert status={isSchemaValid ? 'success' : 'error'} size="xs" py={1} pl={2}>
      <AlertIcon />
    </Alert>
  )
}

export default SchemaValidationMark
