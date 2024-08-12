import { type FC, useMemo } from 'react'
import type { JSONSchema7, JSONSchema7TypeName } from 'json-schema'
import type { IconType } from 'react-icons'
import { Badge, List, ListIcon, ListItem, Tooltip } from '@chakra-ui/react'

import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { DataTypeIcon } from '@/components/rjsf/MqttTransformation/utils/data-type.utils.tsx'

interface JsonSchemaBrowserProps {
  schema: JSONSchema7
}

const JsonSchemaBrowser: FC<JsonSchemaBrowserProps> = (props) => {
  const properties = useMemo(() => {
    return getPropertyListFrom(props.schema)
  }, [props.schema])

  return (
    <List>
      {properties.length > 0 &&
        properties.map((property) => {
          const TypeIcon = DataTypeIcon[(property.type || 'null') as JSONSchema7TypeName satisfies JSONSchema7TypeName]

          return (
            <ListItem key={[...property.path, property.title].join('-')} ml={(property?.path?.length || 0) * 8}>
              <ListIcon as={TypeIcon as IconType} color="green.500" />
              <Tooltip label={[...property.path, property.title].join('/')} placement="top" isDisabled>
                <Badge>{[property.title].join(' . ')}</Badge>
              </Tooltip>
            </ListItem>
          )
        })}
    </List>
  )
}

export default JsonSchemaBrowser
