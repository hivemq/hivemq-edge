import { FC, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { dropTargetForElements } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import {
  Box,
  ButtonGroup,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Code,
  FormControl,
  HStack,
  Textarea,
} from '@chakra-ui/react'
import { RiDeleteBin2Fill, RiFormula } from 'react-icons/ri'

import IconButton from '@/components/Chakra/IconButton.tsx'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx'
import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { FieldMapping } from '@/modules/Mappings/types.ts'
import { getDropZoneBorder } from '@/modules/Theme/utils.ts'

enum DropState {
  IDLE = 'IDLE',
  DRAG_OVER = 'DRAG_OVER',
  COMPLETED = 'COMPLETED',
}

interface MappingInstructionProps {
  property: FlatJSONSchema7
  showTransformation?: boolean
  mapping?: FieldMapping
  onChange?: (source: string | undefined, destination: string) => void
}

const MappingInstruction: FC<MappingInstructionProps> = ({
  property,
  mapping,
  onChange,
  showTransformation = false,
}) => {
  const { t } = useTranslation('components')
  const [state, setState] = useState<DropState>(DropState.IDLE)
  const dropTargetRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const element = dropTargetRef.current
    if (!element) return

    return dropTargetForElements({
      element,
      canDrop: (dropTarget) => {
        return dropTarget.source.data.type === property.type
      },
      onDragEnter: () => {
        setState(DropState.DRAG_OVER)
      },
      onDragLeave: () => {
        setState(DropState.IDLE)
      },
      onDrop: (dropTarget) => {
        setState(DropState.COMPLETED)
        onChange?.(dropTarget.source.data.title as string, property.title as string)
      },
    })
  }, [onChange, property])

  const activeColor = state === DropState.DRAG_OVER || state === DropState.COMPLETED ? 'green' : 'blue.500'
  const backgroundColor = state === DropState.DRAG_OVER ? 'green.100' : 'inherit'

  return (
    <HStack>
      <Card size="sm" variant="outline" w="100%">
        <CardHeader>
          <PropertyItem property={property} hasTooltip />
        </CardHeader>
        <CardBody display="flex" flexDirection="row" gap={2}>
          <Box
            {...getDropZoneBorder(activeColor)}
            backgroundColor={backgroundColor}
            p={4}
            ref={dropTargetRef}
            minW={250}
            data-testid="mapping-instruction-dropzone"
            role="group"
            aria-label={t('rjsf.MqttTransformationField.instructions.dropzone.role')}
          >
            {mapping ? (
              <Code>{mapping.source.propertyPath}</Code>
            ) : (
              t('rjsf.MqttTransformationField.instructions.dropzone.arial-label')
            )}
          </Box>
          <ButtonGroup isAttached size="xs" role="toolbar">
            <IconButton
              aria-label={t('rjsf.MqttTransformationField.instructions.actions.clear.aria-label')}
              icon={<RiDeleteBin2Fill />}
              onClick={() => setState(DropState.IDLE)}
            />
          </ButtonGroup>
        </CardBody>
        {state === DropState.COMPLETED && showTransformation && (
          <CardFooter role="group" aria-label={t('rjsf.MqttTransformationField.instructions.editor.role')}>
            <ButtonGroup isAttached size="xs" isDisabled>
              <IconButton
                aria-label={t('rjsf.MqttTransformationField.instructions.actions.edit.aria-label')}
                icon={<RiFormula />}
              />
            </ButtonGroup>
            <FormControl>
              <Textarea size="xs" aria-label="ssss" />
            </FormControl>
          </CardFooter>
        )}
      </Card>
    </HStack>
  )
}

export default MappingInstruction
