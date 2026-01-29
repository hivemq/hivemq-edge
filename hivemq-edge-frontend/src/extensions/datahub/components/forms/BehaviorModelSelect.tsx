import type { FC, FocusEvent } from 'react'
import { useCallback, useMemo } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import {
  Badge,
  Box,
  Card,
  CardBody,
  FormControl,
  FormLabel,
  HStack,
  Radio,
  RadioGroup,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { getChakra } from '@/components/rjsf/utils/getChakra'
import type { ModelMetadata } from '@datahub/components/forms/behaviorModelMetadata.utils.ts'
import { extractModelMetadata } from '@datahub/components/forms/behaviorModelMetadata.utils.ts'

interface ModelOptionProps {
  model: ModelMetadata
  isSelected: boolean
  onSelect: () => void
}

const ModelOption: FC<ModelOptionProps> = ({ model, isSelected, onSelect }) => {
  const { t } = useTranslation('datahub')

  const handleCardClick = (e: React.MouseEvent) => {
    // Only trigger if not clicking on the radio itself (to avoid double-firing)
    if ((e.target as HTMLElement).tagName !== 'INPUT') {
      onSelect()
    }
  }

  return (
    <Card
      variant={isSelected ? 'filled' : 'outline'}
      size="sm"
      borderWidth={isSelected ? '2px' : '1px'}
      borderColor={isSelected ? 'blue.500' : 'gray.200'}
      _hover={{ borderColor: 'blue.300', cursor: 'pointer' }}
      transition="all 0.2s"
      onClick={handleCardClick}
    >
      <CardBody>
        <HStack spacing={3} align="flex-start">
          <Radio value={model.id} flex="0 0 auto" mt={1} />
          <VStack align="flex-start" spacing={2} flex={1}>
            <HStack spacing={2} width="100%">
              <Text as="b">{model.title}</Text>
              {model.requiresArguments && (
                <Badge colorScheme="orange" fontSize="xs">
                  {t('behaviorModel.badge.requiresArguments')}
                </Badge>
              )}
            </HStack>
            <Text fontSize="sm">{model.description}</Text>
            <HStack spacing={4} fontSize="xs" width="100%">
              <Text>{t('behaviorModel.summary.states', { count: model.stateCount })}</Text>
              <Text>{t('behaviorModel.summary.transitions', { count: model.transitionCount })}</Text>

              {(model.hasSuccessState || model.hasFailedState) && (
                <HStack spacing={1} ml="auto">
                  <Text fontSize="xs">{t('behaviorModel.summary.endStates')}</Text>
                  {model.hasSuccessState && (
                    <Badge colorScheme="green" fontSize="xs">
                      SUCCESS
                    </Badge>
                  )}
                  {model.hasFailedState && (
                    <Badge colorScheme="red" fontSize="xs">
                      FAILED
                    </Badge>
                  )}
                </HStack>
              )}
            </HStack>
          </VStack>
        </HStack>
      </CardBody>
    </Card>
  )
}

export const BehaviorModelSelect = (props: WidgetProps) => {
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const models = useMemo(() => extractModelMetadata(), [])

  const onChange = useCallback(
    (value: string) => {
      props.onChange(value)
    },
    [props]
  )

  const onBlur = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, value)
  const onFocus = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, value)

  return (
    <FormControl
      mb={1}
      {...chakraProps}
      isDisabled={props.disabled || props.readonly}
      isRequired={props.required}
      isReadOnly={props.readonly}
      isInvalid={props.rawErrors && props.rawErrors.length > 0}
    >
      {labelValue(
        <FormLabel htmlFor={props.id} id={`${props.id}-label`}>
          {props.label}
        </FormLabel>,
        props.hideLabel || !props.label
      )}

      <Box onBlur={onBlur} onFocus={onFocus}>
        <RadioGroup
          onChange={onChange}
          value={props.value || undefined}
          id={props.id}
          isDisabled={props.disabled || props.readonly}
        >
          <Stack spacing={3}>
            {models.map((model) => (
              <ModelOption
                key={model.id}
                model={model}
                isSelected={props.value === model.id}
                onSelect={() => onChange(model.id)}
              />
            ))}
          </Stack>
        </RadioGroup>
      </Box>
    </FormControl>
  )
}
