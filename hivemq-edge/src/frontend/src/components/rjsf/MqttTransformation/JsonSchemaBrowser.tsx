import { type FC, useMemo } from 'react'
import type { JSONSchema7 } from 'json-schema'
import { List, ListProps } from '@chakra-ui/react'

import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx'

interface JsonSchemaBrowserProps extends ListProps {
  schema: JSONSchema7
  isDraggable?: boolean
}

const JsonSchemaBrowser: FC<JsonSchemaBrowserProps> = ({ schema, isDraggable = false, ...props }) => {
  const properties = useMemo(() => {
    return getPropertyListFrom(schema)
  }, [schema])

  return (
    <List {...props}>
      {properties.length > 0 &&
        properties.map((property) => {
          return (
            <PropertyItem
              key={[...property.path, property.title].join('-')}
              property={property}
              isDraggable={isDraggable}
            />
          )
        })}
    </List>
  )
}

export default JsonSchemaBrowser
