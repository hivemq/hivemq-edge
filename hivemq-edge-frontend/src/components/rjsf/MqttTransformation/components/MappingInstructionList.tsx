import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
import type { RJSFSchema } from '@rjsf/utils'

import type { ListProps } from '@chakra-ui/react'
import { List, ListItem } from '@chakra-ui/react'

import type { Instruction } from '@/api/__generated__'
import MappingInstruction from '@/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { isReadOnly, toJsonPath } from '@/components/rjsf/MqttTransformation/utils/data-type.utils'

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
    return getPropertyListFrom(schema)
  }, [schema])

  useEffect(() => {
    // Auto-remove instructions that target readonly properties
    if (!instructions || instructions.length === 0) return

    const readonlyPaths = new Set(properties.filter(isReadOnly).map((p) => ['$', ...p.path, p.key].join('.')))

    if (readonlyPaths.size === 0) return

    const cleanedInstructions = instructions.filter((instruction) => !readonlyPaths.has(instruction.destination))

    if (cleanedInstructions.length !== instructions.length) {
      onChange?.(cleanedInstructions)
    }
  }, [instructions, properties, onChange])

  return (
    <List {...props} gap={2}>
      {properties.map((property) => {
        const instructionIndex = instructions
          ? instructions.findIndex((instruction) => {
              const fullPath = ['$', ...property.path, property.key].join('.')
              return instruction.destination === fullPath
            })
          : -1
        return (
          <ListItem key={property.key}>
            <MappingInstruction
              showTransformation={showTransformation}
              showPathAsName={true}
              property={property}
              instruction={instructionIndex !== -1 ? instructions?.[instructionIndex] : undefined}
              onChange={(source, destination, sourceRef) => {
                let newMappings = [...(instructions || [])]
                if (source) {
                  const newItem: Instruction = {
                    source: toJsonPath(source),
                    destination: toJsonPath(destination),
                    sourceRef: sourceRef,
                  }
                  if (instructionIndex !== -1) {
                    newMappings[instructionIndex] = newItem
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
