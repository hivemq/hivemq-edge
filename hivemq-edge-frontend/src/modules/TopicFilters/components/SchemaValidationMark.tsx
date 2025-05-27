import type { FC } from 'react'
import { useMemo } from 'react'
import { Alert, AlertIcon } from '@chakra-ui/react'

import type { TopicFilter } from '@/api/__generated__'
import type { SchemaHandler } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'
import { validateSchemaFromDataURI } from '@/modules/TopicFilters/utils/topic-filter.schema.ts'

interface SchemaValidationMarkProps {
  topicFilter: TopicFilter
}

const SchemaValidationMark: FC<SchemaValidationMarkProps> = ({ topicFilter }) => {
  const schemaHandler = useMemo<SchemaHandler>(
    () => validateSchemaFromDataURI(topicFilter.schema),
    [topicFilter.schema]
  )

  return (
    <Alert status={schemaHandler.status} size="xs" py={1} pl={2} w={10}>
      <AlertIcon />
    </Alert>
  )
}

export default SchemaValidationMark
