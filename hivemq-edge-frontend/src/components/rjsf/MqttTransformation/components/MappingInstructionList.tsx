import type { FC } from 'react'
import { useMemo } from 'react'
import type { RJSFSchema } from '@rjsf/utils'

import type { ListProps } from '@chakra-ui/react'
import { List, ListItem } from '@chakra-ui/react'

import type { Instruction } from '@/api/__generated__'
import MappingInstruction from '@/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx'
import { getPropertyListFrom } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import {
  filterReadOnlyInstructions,
  isReadOnly,
  toJsonPath,
} from '@/components/rjsf/MqttTransformation/utils/data-type.utils'

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
  const { properties, validInstructions } = useMemo(() => {
    const allProperties = getPropertyListFrom(schema)
    // Persisted instructions may still target a read-only property (created before the property became
    // read-only, or before it was hidden); they are pruned against the full list so they cannot ghost-match.
    const validInstructions = filterReadOnlyInstructions(instructions, allProperties)
    // A read-only property is not a writable destination, so it is hidden rather than rendered as a
    // neutralised card (EDG-59). This applies to both consumers: in the southbound editor the envelope is
    // already gone, so it only fires for a genuinely read-only field inside the value; in the combiner
    // destination editor, schemas inferred from northbound documents routinely carry read-only envelope
    // fields (tagName, timestamp, metadata) — those were never mappable and are now hidden too.
    const properties = allProperties.filter((property) => !isReadOnly(property))
    return { properties, validInstructions }
  }, [schema, instructions])

  return (
    <List {...props} gap={2}>
      {properties.map((property) => {
        const instructionIndex = validInstructions.findIndex((instruction) => {
          const fullPath = ['$', ...property.path, property.key].join('.')
          return instruction.destination === fullPath
        })
        return (
          <ListItem key={property.key}>
            <MappingInstruction
              showTransformation={showTransformation}
              showPathAsName={true}
              property={property}
              instruction={instructionIndex !== -1 ? validInstructions[instructionIndex] : undefined}
              onChange={(source, destination, sourceRef) => {
                let newMappings = [...validInstructions]
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
