import type { FC } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { dropTargetForElements } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import {
  Alert,
  AlertIcon,
  Box,
  ButtonGroup,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Code,
  FormControl,
  HStack,
  Text,
  Textarea,
} from '@chakra-ui/react'
import { RiDeleteBin2Fill, RiFormula } from 'react-icons/ri'

import type { DataIdentifierReference, Instruction } from '@/api/__generated__'
import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import IconButton from '@/components/Chakra/IconButton.tsx'
import PropertyItem from '@/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx'
import {
  formatPath,
  fromJsonPath,
  isMappingSupported,
  toJsonPath,
} from '@/components/rjsf/MqttTransformation/utils/data-type.utils.ts'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import { getDropZoneBorder } from '@/modules/Theme/utils.ts'

enum DropState {
  IDLE = 'IDLE',
  DRAG_OVER = 'DRAG_OVER',
  COMPLETED = 'COMPLETED',
}

interface MappingInstructionProps {
  property: FlatJSONSchema7
  showTransformation?: boolean
  instruction?: Instruction
  showPathAsName?: boolean
  onChange?: (source: string | undefined, destination: string, sourceRef?: DataIdentifierReference) => void
}

const MappingInstruction: FC<MappingInstructionProps> = ({
  property,
  instruction,
  onChange,
  showTransformation = false,
  showPathAsName = false,
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
        return dropTarget.source.data.type === property.type && dropTarget.source.data.arrayType === property.arrayType
      },
      onDragEnter: () => {
        setState(DropState.DRAG_OVER)
      },
      onDragLeave: () => {
        setState(DropState.IDLE)
      },
      onDrop: (dropTarget) => {
        setState(DropState.COMPLETED)
        const target = dropTarget.source.data as unknown as FlatJSONSchema7 & {
          dataReference: DataReference | undefined
        }

        const sourceRef: DataIdentifierReference | undefined = target.dataReference
          ? { id: target.dataReference.id, type: target.dataReference.type }
          : undefined
        onChange?.(
          [...target.path, target.key].join('.') as string,
          [...property.path, property.key].join('.') as string,
          sourceRef
        )
      },
    })
  }, [onChange, property])

  const activeColor = state === DropState.DRAG_OVER || state === DropState.COMPLETED ? 'green' : 'gray.500'
  const backgroundColor = state === DropState.DRAG_OVER ? 'green.100' : 'inherit'
  const isSupported = isMappingSupported(property)

  const onHandleClear = () => {
    setState(DropState.IDLE)
    const fullPath = [...property.path, property.key].join('.')
    onChange?.(undefined, toJsonPath(fullPath))
  }

  if (!isSupported)
    return (
      <Card size="sm" variant="outline" w="100%">
        <CardHeader as={HStack} justifyContent="space-between">
          <PropertyItem property={property} hasTooltip hasPathAsName={showPathAsName} />
          <Alert status="warning" size="sm" variant="left-accent" w="140px">
            <AlertIcon />
            {t('rjsf.MqttTransformationField.validation.notSupported')}
          </Alert>
        </CardHeader>
      </Card>
    )

  return (
    <HStack>
      <Card size="sm" variant="outline" w="100%">
        <CardHeader>
          <PropertyItem property={property} hasTooltip hasPathAsName={showPathAsName} />
        </CardHeader>

        <CardBody display="flex" flexDirection="row" gap={2}>
          <Box
            {...getDropZoneBorder(activeColor)}
            backgroundColor={backgroundColor}
            p={4}
            py={2}
            ref={dropTargetRef}
            data-testid="mapping-instruction-dropzone"
            role="group"
            aria-label={t('rjsf.MqttTransformationField.instructions.dropzone.role')}
            flex={3}
          >
            {instruction?.source ? (
              <Code>{formatPath(fromJsonPath(instruction.source))}</Code>
            ) : (
              <Text as="span" color="var(--chakra-colors-chakra-placeholder-color)" userSelect="none">
                {t('rjsf.MqttTransformationField.instructions.dropzone.arial-label')}
              </Text>
            )}
          </Box>
          <ButtonGroup isAttached size="xs" role="toolbar">
            <IconButton
              aria-label={t('rjsf.MqttTransformationField.instructions.actions.clear.aria-label')}
              icon={<RiDeleteBin2Fill />}
              onClick={onHandleClear}
              isDisabled={Boolean(!instruction?.source)}
            />
          </ButtonGroup>
          <Alert status={instruction?.source ? 'success' : 'error'} size="sm" variant="left-accent" w="140px">
            <AlertIcon />
            {instruction?.source
              ? t('rjsf.MqttTransformationField.validation.matching')
              : t('rjsf.MqttTransformationField.validation.required')}
          </Alert>
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
