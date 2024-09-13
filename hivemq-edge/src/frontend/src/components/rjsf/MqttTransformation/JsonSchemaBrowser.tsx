import { type FC, useMemo } from 'react'
import type { JSONSchema7 } from 'json-schema'
import { Heading, List, ListProps } from '@chakra-ui/react'

import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx'

interface JsonSchemaBrowserProps extends ListProps {
  schema: JSONSchema7
  isDraggable?: boolean
  hasExamples?: boolean
}

const JsonSchemaBrowser: FC<JsonSchemaBrowserProps> = ({
  schema,
  isDraggable = false,
  hasExamples = false,
  ...props
}) => {
  const properties = useMemo(() => {
    return getPropertyListFrom(schema)
  }, [schema])

  return (
    <>
      {schema.title && (
        <Heading as="h4" size="sm">
          {schema.title}
        </Heading>
      )}
      <List {...props}>
        {properties.length > 0 &&
          properties.map((property) => {
            return (
              <PropertyItem
                key={[...property.path, property.title].join('-')}
                property={property}
                isDraggable={isDraggable}
                hasExamples={hasExamples}
              />
            )
          })}
      </List>
    </>
  )
}

export default JsonSchemaBrowser
