import type { ChangeEvent, FC } from 'react'
import type { BaseInputTemplateProps } from '@rjsf/utils'
import { getInputProps } from '@rjsf/utils'
import { FormControl, Input } from '@chakra-ui/react'

export const CompactBaseInputTemplate: FC<BaseInputTemplateProps> = (props) => {
  const {
    id,
    type,
    value,
    label,
    schema,
    onChange,
    onChangeOverride,
    onBlur,
    onFocus,
    options,
    required,
    readonly,
    rawErrors,
    autofocus,
    placeholder,
  } = props
  const inputProps = getInputProps(schema, type, options)

  const _onChange = ({ target: { value } }: ChangeEvent<HTMLInputElement>) =>
    onChange(value === '' ? options.emptyValue : value)

  return (
    <FormControl isRequired={required} isReadOnly={readonly} isInvalid={rawErrors && rawErrors.length > 0}>
      <Input
        size="sm"
        id={id}
        name={id}
        value={value || value === 0 ? value : ''}
        onChange={onChangeOverride || _onChange}
        onBlur={({ target }) => onBlur(id, target && target.value)}
        onFocus={({ target }) => onFocus(id, target && target.value)}
        autoFocus={autofocus}
        placeholder={placeholder}
        {...inputProps}
        aria-label={label}
      />
    </FormControl>
  )
}
