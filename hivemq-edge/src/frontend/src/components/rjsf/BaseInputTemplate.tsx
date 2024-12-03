import { ChangeEvent, FC, FocusEvent } from 'react'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { ariaDescribedByIds, BaseInputTemplateProps, examplesId, getInputProps, labelValue } from '@rjsf/utils'
import { FormControl, FormLabel, Input } from '@chakra-ui/react'

// TODO[RJSF] Bug with file accept; see https://github.com/rjsf-team/react-jsonschema-form/issues/4404
export const BaseInputTemplate: FC<BaseInputTemplateProps> = (props) => {
  const { schema } = props

  const inputProps = getInputProps(schema, props.type, props.options)
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  if (props.accept && props.type === 'file') {
    // @ts-ignore
    inputProps.accept = props.accept
  }

  const _onChange = ({ target: { value } }: ChangeEvent<HTMLInputElement>) =>
    props.onChange(value === '' ? props.options.emptyValue : value)
  const _onBlur = ({ target }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, target && target.value)
  const _onFocus = ({ target }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, target && target.value)

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
      <Input
        id={props.id}
        name={props.id}
        value={props.value || props.value === 0 ? props.value : ''}
        onChange={props.onChangeOverride || _onChange}
        onBlur={_onBlur}
        onFocus={_onFocus}
        autoFocus={props.autofocus}
        placeholder={props.placeholder}
        {...inputProps}
        list={schema.examples ? examplesId(props.id) : undefined}
        aria-describedby={ariaDescribedByIds(props.id, !!schema.examples)}
      />
      {Array.isArray(schema.examples) ? (
        <datalist id={examplesId(props.id)}>
          {(schema.examples as string[])
            .concat(schema.default && !schema.examples.includes(schema.default) ? ([schema.default] as string[]) : [])
            .map((example) => {
              return <option key={example} value={example} />
            })}
        </datalist>
      ) : null}
    </FormControl>
  )
}
