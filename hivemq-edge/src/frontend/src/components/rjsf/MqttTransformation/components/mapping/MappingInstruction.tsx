import { FC, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { dropTargetForElements } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import {
  ButtonGroup,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Code,
  FormControl,
  HStack,
  List,
  Textarea,
} from '@chakra-ui/react'
import { RiDeleteBin2Fill, RiFormula } from 'react-icons/ri'

import IconButton from '@/components/Chakra/IconButton.tsx'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx'
import { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { Mapping } from '@/modules/Mappings/types.ts'
import { getDropZoneBorder } from '@/modules/Theme/utils.ts'

enum DropState {
  IDLE = 'IDLE',
  DRAG_OVER = 'DRAG_OVER',
  COMPLETED = 'COMPLETED',
}

interface MappingInstructionProps {
  property: FlatJSONSchema7
  showTransformation?: boolean
  mapping?: Mapping
  onChange?: (source: string, destination: string) => void
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
      <Card size="sm" variant="outline" flex={1}>
        <CardHeader>
          <List>
            <PropertyItem property={property} />
          </List>
        </CardHeader>
        <CardBody
          {...getDropZoneBorder(activeColor)}
          backgroundColor={backgroundColor}
          m={2}
          p={4}
          ref={dropTargetRef}
          minW={250}
          data-testid="mapping-instruction-dropzone"
        >
          {mapping ? (
            <Code>{mapping.source.join(' ')}</Code>
          ) : (
            t('rjsf.MqttTransformationField.instructions.dropzone.arial-label')
          )}
        </CardBody>
        {state === DropState.COMPLETED && showTransformation && (
          <CardFooter>
            <ButtonGroup isAttached size="xs" isDisabled>
              <IconButton
                aria-label={t('rjsf.MqttTransformationField.instructions.actions.edit.aria-label')}
                icon={<RiFormula />}
              />
            </ButtonGroup>
            <FormControl>
              <Textarea size="xs" aria-label="ssss" value="`${veniam}${campana}`" />
            </FormControl>
          </CardFooter>
        )}
      </Card>
      <ButtonGroup isAttached size="xs">
        <IconButton
          aria-label={t('rjsf.MqttTransformationField.instructions.actions.clear.aria-label')}
          icon={<RiDeleteBin2Fill />}
          onClick={() => setState(DropState.IDLE)}
        />
      </ButtonGroup>
    </HStack>
  )
}

export default MappingInstruction
