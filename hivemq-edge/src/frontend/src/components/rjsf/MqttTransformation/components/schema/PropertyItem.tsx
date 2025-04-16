import type { FC } from 'react'
import { useEffect, useRef } from 'react'
import type { IconType } from 'react-icons'
import { useTranslation } from 'react-i18next'
import type { JSONSchema7TypeName } from 'json-schema'
import { draggable } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import { Badge, Code, HStack, Tooltip, Box, Icon, Text, VStack } from '@chakra-ui/react'

import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import { DataTypeIcon } from '@/components/rjsf/MqttTransformation/utils/data-type.utils.ts'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { EDGE_HOTKEY } from '@/hooks/useAccessibleDraggable/type'
import { useAccessibleDraggable } from '@/hooks/useAccessibleDraggable'

interface PropertyItemProps {
  property: FlatJSONSchema7
  isDraggable?: boolean
  hasTooltip?: boolean
  hasExamples?: boolean
  hasDescription?: boolean
  dataReference?: DataReference
  hasPathAsName?: boolean
}

const PropertyItem: FC<PropertyItemProps> = ({
  property,
  isDraggable = false,
  hasTooltip = false,
  hasExamples = false,
  hasDescription = false,
  hasPathAsName = false,
  dataReference,
}) => {
  const { t } = useTranslation('components')
  const draggableRef = useRef<HTMLDivElement | null>(null)
  const { startDragging } = useAccessibleDraggable()

  useEffect(() => {
    if (!isDraggable) return
    const element = draggableRef.current
    if (!element) return
    return draggable({
      element,
      getInitialData: () => ({ ...property, dataReference }),
    })
  }, [dataReference, isDraggable, property, property.type])

  const TypeIcon = DataTypeIcon[(property.type || 'null') as JSONSchema7TypeName satisfies JSONSchema7TypeName]
  const type = t('GenericSchema.data.type', {
    context: property.type || 'null',
    arrayType: property.arrayType || 'object',
  })
  // TODO[NVL] key should be use for mapping. But what to use for display? key or title or both ?
  const propertyName = property.title
  const path = [...property.path, property.key].join('.')

  // TODO[NVL] Only extracting the first value of any examples. Might want to provide something more user-friendly
  const examples = Array.isArray(property.examples) ? property.examples[0] : property.examples

  return (
    <HStack
      key={path}
      data-type={property.type as string}
      data-path={path}
      tabIndex={isDraggable ? 0 : undefined}
      py="3px"
      justifyContent="flex-end"
      alignItems="flex-start"
      role="group"
      aria-label={t('GenericSchema.structure.property')}
      onKeyUp={(event) => {
        if (event.key === EDGE_HOTKEY.ENTER && isDraggable) {
          startDragging?.({ property, dataReference })
        }
      }}
    >
      <HStack gap={0} ref={draggableRef} flex={1}>
        <Tooltip label={type} placement="top" hasArrow>
          <Box marginInlineEnd={2} aria-label={type} role="img" display="flex" data-testid="property-type">
            <Icon as={TypeIcon as IconType} color="green.500" m={0} />
            {property.arrayType && (
              <Icon
                as={
                  DataTypeIcon[
                    (property.arrayType || 'null') as JSONSchema7TypeName satisfies JSONSchema7TypeName
                  ] as IconType
                }
                color="green.500"
                m={0}
              />
            )}
          </Box>
        </Tooltip>
        <Tooltip label={path} placement="top" isDisabled={!hasTooltip} hasArrow>
          <Badge data-testid="property-name" aria-label={path}>
            {hasPathAsName ? path : propertyName}
          </Badge>
        </Tooltip>
      </HStack>
      <VStack gap={0} alignItems="flex-end">
        {examples && hasExamples && (
          <Code
            aria-label={t('GenericSchema.structure.example')}
            data-testid="property-example"
            size="xs"
            variant="none"
            fontSize="xs"
            overflow="hidden"
            textOverflow="ellipsis"
            whiteSpace="nowrap"
          >
            {examples.toString()}
          </Code>
        )}
        {hasDescription && (
          <Text fontSize="sm" textAlign="end">
            {property.description}
          </Text>
        )}
      </VStack>
    </HStack>
  )
}

export default PropertyItem
