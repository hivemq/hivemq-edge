import type { FC } from 'react'
import { useMemo } from 'react'
import type { RJSFSchema } from '@rjsf/utils'

import type { ListProps } from '@chakra-ui/react'
import { List, ListItem } from '@chakra-ui/react'

import type { Instruction } from '@/api/__generated__'
import { filterSupportedProperties } from '@/components/rjsf/MqttTransformation/utils/data-type.utils.ts'
import MappingInstruction from '@/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'

interface MappingEditorProps extends Omit<ListProps, 'onChange'> {
  instructions: Instruction[]
  schema: RJSFSchema
  showTransformation?: boolean
  onChange?: (v: Instruction[] | undefined) => void
}

export const MappingInstructionList: FC<MappingEditorProps> = ({
  instructions,
  schema,
  onChange,
  showTransformation = false,
  ...props
}) => {
  const properties = useMemo(() => {
    const allProperties = getPropertyListFrom(schema)
    return allProperties.filter(filterSupportedProperties)
  }, [schema])

  return (
    <List {...props}>
      {properties.map((property) => {
        const instruction = instructions
          ? instructions.findIndex((instruction) => instruction.destination === property.key)
          : -1
        return (
          <ListItem key={property.key}>
            <MappingInstruction
              showTransformation={showTransformation}
              property={property}
              instruction={instruction !== -1 ? instructions?.[instruction] : undefined}
              onChange={(source, destination, sourceRef) => {
                let newMappings = [...(instructions || [])]
                if (source) {
                  const newItem: Instruction = {
                    source: source,
                    destination: destination,
                    sourceRef: sourceRef,
                  }
                  if (instruction !== -1) {
                    newMappings[instruction] = newItem
                  } else newMappings.push(newItem)
                } else {
                  newMappings = newMappings.filter((mapped) => mapped.destination !== destination)
                }

                onChange?.(newMappings)
              }}
            />
          </ListItem>
        )
      })}
    </List>
  )
}
