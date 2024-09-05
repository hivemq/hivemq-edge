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
import { Mapping } from '@/modules/Subscriptions/types.ts'

enum DropState {
  IDLE = 'IDLE',
  DRAG_OVER = 'DRAG_OVER',
  COMPLETED = 'COMPLETED',
}

const getDropZoneBorder = (color: string, state: DropState) => {
  const activeColor = state === DropState.DRAG_OVER || state === DropState.COMPLETED ? 'green' : color
  return {
    bgGradient: `repeating-linear(0deg, ${activeColor}, ${activeColor} 10px, transparent 10px, transparent 20px, ${activeColor} 20px), repeating-linear-gradient(90deg, ${activeColor}, ${activeColor} 10px, transparent 10px, transparent 20px, ${activeColor} 20px), repeating-linear-gradient(180deg, ${activeColor}, ${activeColor} 10px, transparent 10px, transparent 20px, ${activeColor} 20px), repeating-linear-gradient(270deg, ${activeColor}, ${activeColor} 10px, transparent 10px, transparent 20px, ${activeColor} 20px)`,
    backgroundSize: '2px 100%, 100% 2px, 2px 100% , 100% 2px',
    backgroundPosition: '0 0, 0 0, 100% 0, 0 100%',
    backgroundRepeat: 'no-repeat',
    borderRadius: '4px',
    backgroundColor: state === DropState.DRAG_OVER ? 'green.100' : 'inherit',
  }
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
  onChange: XXXXX,
  showTransformation = false,
}) => {
  const { t } = useTranslation('components')
  const [state, setState] = useState<DropState>(DropState.IDLE)
  const ref = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const element = ref.current
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
        XXXXX?.(dropTarget.source.data.title as string, property.title as string)
      },
    })
  }, [XXXXX, property])

  return (
    <HStack>
      <Card size="sm" variant="outline" flex={1}>
        <CardHeader>
          <List>
            <PropertyItem property={property} />
          </List>
        </CardHeader>
        <CardBody {...getDropZoneBorder('blue', state)} m={2} p={4} ref={ref} minW={250}>
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
