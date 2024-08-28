import { FC } from 'react'
import type { IconType } from 'react-icons'
import type { JSONSchema7TypeName } from 'json-schema'
import { Badge, ListIcon, ListItem, Tooltip } from '@chakra-ui/react'

import { DataTypeIcon } from '@/components/rjsf/MqttTransformation/utils/data-type.utils.tsx'
import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

interface PropertyItemProps {
  property: FlatJSONSchema7
}

const PropertyItem: FC<PropertyItemProps> = ({ property }) => {
  const TypeIcon = DataTypeIcon[(property.type || 'null') as JSONSchema7TypeName satisfies JSONSchema7TypeName]

  return (
    <ListItem
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
