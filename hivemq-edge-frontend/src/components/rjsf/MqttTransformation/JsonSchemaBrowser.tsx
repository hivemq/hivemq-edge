import { type FC, useMemo } from 'react'
import type { JSONSchema7 } from 'json-schema'
import type { ListProps } from '@chakra-ui/react'
import { Heading, List, ListItem } from '@chakra-ui/react'

import { DataIdentifierReference } from '@/api/__generated__'
import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { PLCTag, TopicFilter } from '@/components/MQTT/EntityTag'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx'

interface JsonSchemaBrowserProps extends ListProps {
  schema: JSONSchema7
  isDraggable?: boolean
  hasExamples?: boolean
  isTagShown?: boolean
  dataReference?: DataReference
}

const JsonSchemaBrowser: FC<JsonSchemaBrowserProps> = ({
  schema,
  isDraggable = false,
  hasExamples = false,
  isTagShown = false,
  dataReference,
  ...props
}) => {
  const properties = useMemo(() => {
    return getPropertyListFrom(schema)
  }, [schema])

  return (
    <>
      {schema.title && (
        <Heading as="h3" size="sm">
          {!isTagShown && schema.title}
          {isTagShown && dataReference?.type === DataIdentifierReference.type.TAG && (
            <PLCTag tagTitle={dataReference?.id} mr={3} />
          )}
          {isTagShown && dataReference?.type === DataIdentifierReference.type.TOPIC_FILTER && (
            <TopicFilter tagTitle={dataReference?.id} mr={3} />
          )}
        </Heading>
      )}
      <List {...props}>
        {properties.map((property) => {
          return (
            <ListItem key={[...property.path, property.key].join('-')} ml={(property?.path?.length || 0) * 8}>
              <PropertyItem
                property={property}
                dataReference={dataReference}
                isDraggable={isDraggable}
                hasExamples={hasExamples}
                hasTooltip
              />
            </ListItem>
          )
        })}
      </List>
    </>
  )
}

export default JsonSchemaBrowser
