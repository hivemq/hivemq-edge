/* generated from RJSF for local bug fix -- do no edit */
/* istanbul ignore file -- @preserve */
import type { FocusEvent } from 'react'
import {
  NumberInput,
  NumberDecrementStepper,
  NumberIncrementStepper,
  NumberInputField,
  NumberInputStepper,
  FormControl,
  FormLabel,
} from '@chakra-ui/react'
import type { WidgetProps } from '@rjsf/utils'
import { ariaDescribedByIds, labelValue } from '@rjsf/utils'
import { getChakra } from '../utils/getChakra'

/**
 * @override This is a replacement for the original widget
 *   - fix a bug with the label id
 */
export default function UpDownWidget<T>(props: WidgetProps) {
  const { id, uiSchema, readonly, disabled, label, hideLabel, value, onChange, onBlur, onFocus, rawErrors, required } =
    props

  const chakraProps = getChakra({ uiSchema: uiSchema })

  const _onChange = (value: string | number) => onChange(value)
  const _onBlur = ({ target }: FocusEvent<HTMLInputElement>) => onBlur(id, target && target.value)
  const _onFocus = ({ target }: FocusEvent<HTMLInputElement>) => onFocus(id, target && target.value)

  return (
    <FormControl
      mb={1}
      {...chakraProps}
      isDisabled={disabled || readonly}
      isRequired={required}
      isReadOnly={readonly}
      isInvalid={rawErrors && rawErrors.length > 0}
    >
      {labelValue(
        <FormLabel htmlFor={id} id={`${id}-label`}>
          {label}
        </FormLabel>,
        hideLabel || !label
      )}
      <NumberInput
        value={value ?? ''}
        onChange={_onChange}
        onBlur={_onBlur}
        onFocus={_onFocus}
        aria-describedby={ariaDescribedByIds<T>(id)}
      >
        <NumberInputField id={id} name={id} />
        <NumberInputStepper>
          <NumberIncrementStepper />
          <NumberDecrementStepper />
        </NumberInputStepper>
      </NumberInput>
    </FormControl>
  )
}
