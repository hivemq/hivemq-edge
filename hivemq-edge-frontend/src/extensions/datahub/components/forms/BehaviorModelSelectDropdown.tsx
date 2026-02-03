import type { FocusEvent } from 'react'
import { useCallback, useMemo } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { Badge, FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'
import type { OnChangeValue, SingleValueProps, OptionProps } from 'chakra-react-select'
import { Select, chakraComponents } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

import { getChakra } from '@/components/rjsf/utils/getChakra'
import type { ModelMetadata } from '@datahub/components/forms/behaviorModelMetadata.utils.ts'
import { extractModelMetadata } from '@datahub/components/forms/behaviorModelMetadata.utils.ts'

const SingleValue = (props: SingleValueProps<ModelMetadata>) => {
  const { t } = useTranslation('datahub')

  return (
    <chakraComponents.SingleValue {...props}>
      <HStack spacing={2}>
        <Text as="b">{props.data.title}</Text>
        {props.data.requiresArguments && (
          <Badge colorScheme="orange" fontSize="xs">
            {t('behaviorModel.badge.requiresArguments')}
          </Badge>
        )}
      </HStack>
    </chakraComponents.SingleValue>
  )
}

const Option = (props: OptionProps<ModelMetadata>) => {
  const { t } = useTranslation('datahub')
  const { isSelected, ...rest } = props
  const [selectedModel] = props.getValue()

  return (
    <chakraComponents.Option {...rest} isSelected={selectedModel && selectedModel.id === props.data.id}>
      <VStack align="flex-start" spacing={2} width="100%">
        <HStack spacing={2} width="100%">
          <Text as="b" flex={1}>
            {props.data.title}
          </Text>
          {props.data.requiresArguments && (
            <Badge colorScheme="orange" fontSize="xs">
              {t('behaviorModel.badge.requiresArguments')}
            </Badge>
          )}
        </HStack>

        <Text fontSize="sm">{props.data.description}</Text>

        <HStack spacing={4} fontSize="xs" width="100%">
          <Text>{t('behaviorModel.summary.states', { count: props.data.stateCount })}</Text>
          <Text>{t('behaviorModel.summary.transitions', { count: props.data.transitionCount })}</Text>

          {(props.data.hasSuccessState || props.data.hasFailedState) && (
            <HStack spacing={1} ml="auto">
              <Text fontSize="xs">{t('behaviorModel.summary.endStates')}</Text>
              {props.data.hasSuccessState && (
                <Badge colorScheme="green" fontSize="xs">
                  SUCCESS
                </Badge>
              )}
              {props.data.hasFailedState && (
                <Badge colorScheme="red" fontSize="xs">
                  FAILED
                </Badge>
              )}
            </HStack>
          )}
        </HStack>
      </VStack>
    </chakraComponents.Option>
  )
}

export const BehaviorModelSelectDropdown = (props: WidgetProps) => {
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const models = useMemo(() => extractModelMetadata(), [])

  const onChange = useCallback<(newValue: OnChangeValue<ModelMetadata, false>) => void>(
    (newValue) => {
      props.onChange(newValue?.id || undefined)
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

      <Select<ModelMetadata>
        isClearable
        instanceId={props.id}
        id={`${props.id}-container`}
        inputId={props.id}
        isRequired={props.required}
        isDisabled={props.disabled || props.readonly}
        options={models}
        value={models.find((e) => e.id === props.value)}
        getOptionValue={(option) => option.id}
        getOptionLabel={(option) => option.title}
        components={{
          Option,
          SingleValue,
        }}
        onChange={onChange}
        onBlur={onBlur}
        onFocus={onFocus}
      />
    </FormControl>
  )
}
