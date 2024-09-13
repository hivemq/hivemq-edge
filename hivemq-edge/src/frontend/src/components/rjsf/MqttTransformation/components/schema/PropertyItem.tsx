import { FC, useEffect, useRef } from 'react'
import type { IconType } from 'react-icons'
import type { JSONSchema7TypeName } from 'json-schema'
import { draggable } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import { Badge, Code, HStack, ListIcon, ListItem, Tooltip } from '@chakra-ui/react'

import { DataTypeIcon } from '@/components/rjsf/MqttTransformation/utils/data-type.utils.tsx'
import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

interface PropertyItemProps {
  property: FlatJSONSchema7
  isDraggable?: boolean
  hasTooltip?: boolean
  hasExamples?: boolean
}

const PropertyItem: FC<PropertyItemProps> = ({
  property,
  isDraggable = false,
  hasTooltip = false,
  hasExamples = false,
}) => {
  const draggableRef = useRef<HTMLLIElement | null>(null)

  useEffect(() => {
    if (!isDraggable) return
    const element = draggableRef.current
    if (!element) return
    return draggable({
      element,
      getInitialData: () => ({ ...property }),
    })
  }, [isDraggable, property, property.type])

  const TypeIcon = DataTypeIcon[(property.type || 'null') as JSONSchema7TypeName satisfies JSONSchema7TypeName]
  const path = [...property.path, property.title].join('.')

  return (
    <ListItem
      ref={draggableRef}
      key={[...property.path, property.title].join('-')}
      ml={(property?.path?.length || 0) * 8}
      data-type={property.type as string}
      data-path={path}
      tabIndex={isDraggable ? 0 : undefined}
    >
      <HStack py="3px">
        <ListIcon as={TypeIcon as IconType} color="green.500" />
        <Tooltip label={path} placement="top" isDisabled={!hasTooltip}>
          <Badge data-testid="property-name">{[property.title].join(' . ')}</Badge>
        </Tooltip>
        {property.examples && (
          <Code data-testid="property-example" size="xs" variant="none" fontSize="xs">
            {property.examples.toString()}
          </Code>
        )}
      </HStack>
    </ListItem>
  )
}

export default PropertyItem
