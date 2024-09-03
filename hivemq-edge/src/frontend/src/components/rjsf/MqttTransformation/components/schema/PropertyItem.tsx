import { FC, useEffect, useRef } from 'react'
import type { IconType } from 'react-icons'
import type { JSONSchema7TypeName } from 'json-schema'
import { draggable } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import { Badge, ListIcon, ListItem, Tooltip } from '@chakra-ui/react'

import { DataTypeIcon } from '@/components/rjsf/MqttTransformation/utils/data-type.utils.tsx'
import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

interface PropertyItemProps {
  property: FlatJSONSchema7
}

const PropertyItem: FC<PropertyItemProps> = ({ property }) => {
  const ref = useRef<HTMLLIElement | null>(null)

  useEffect(() => {
    const element = ref.current
    if (!element) return
    return draggable({
      element,
      getInitialData: () => ({ taskId: property.type }),
    })
  }, [property.type])

  const TypeIcon = DataTypeIcon[(property.type || 'null') as JSONSchema7TypeName satisfies JSONSchema7TypeName]

  return (
    <ListItem
      ref={ref}
      key={[...property.path, property.title].join('-')}
      ml={(property?.path?.length || 0) * 8}
      data-type={property.type as string}
    >
      <ListIcon as={TypeIcon as IconType} color="green.500" />
      <Tooltip label={[...property.path, property.title].join('/')} placement="top" isDisabled>
        <Badge>{[property.title].join(' . ')}</Badge>
      </Tooltip>
    </ListItem>
  )
}

export default PropertyItem
