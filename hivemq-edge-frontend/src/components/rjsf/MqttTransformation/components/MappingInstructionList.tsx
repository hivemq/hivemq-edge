import type { FC } from 'react'
import { useEffect, useMemo } from 'react'
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

  // Pruning only inside the renderer would leave the stale instruction in the parent's form data: it is
  // invisible here but still submitted and still executed. Hiding the read-only card (above) makes that harder
  // to notice, not easier, so the sanitised list is pushed up as soon as the destination schema resolves rather
  // than waiting for the user to happen to edit some other, visible mapping.
  const hasPrunedInstructions = validInstructions.length !== instructions.length

  useEffect(() => {
    // filterReadOnlyInstructions only ever removes entries, so a length difference is exactly "something was
    // pruned" — no deep comparison needed.
    if (hasPrunedInstructions) onChange?.(validInstructions)
    // onChange is deliberately not a dependency: parents pass a fresh closure on every render, and including it
    // would re-fire this effect on each one for as long as the parent has not persisted the sanitised list.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasPrunedInstructions, validInstructions])

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
