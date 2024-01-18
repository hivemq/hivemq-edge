import { FC } from 'react'
import Form from '@rjsf/chakra-ui'
import { FormProps } from '@rjsf/core'
import {
  DescriptionFieldProps,
  FieldTemplateProps,
  FormContextType,
  RJSFSchema,
  StrictRJSFSchema,
  getTemplate,
  getUiOptions,
} from '@rjsf/utils'
import { GenericObjectType } from '@rjsf/utils/src/types.ts'
import validator from '@rjsf/validator-ajv8'
import { Box, FormControl, Text } from '@chakra-ui/react'

// Override to fix bug with nested p
export function DescriptionFieldTemplate<
  T = unknown,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = GenericObjectType
>({ description, id }: DescriptionFieldProps<T, S, F>) {
  if (!description) {
    return null
  }

  if (typeof description === 'string') {
    return (
      <Text id={id} mt={2} mb={4}>
        {description}
      </Text>
    )
  }

  return <>{description}</>
}

// Override to fix bug with nested p
// Override to fix conditional rendering of either error or description
export function FieldTemplate<
  T = unknown,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = GenericObjectType
>(props: FieldTemplateProps<T, S, F>) {
  const {
    id,
    children,
    classNames,
    style,
    disabled,
    displayLabel,
    hidden,
    label,
    onDropPropertyClick,
    onKeyChange,
    readonly,
    registry,
    required,
    rawErrors = [],
    errors,
    help,
    description,
    rawDescription,
    schema,
    uiSchema,
  } = props
  const uiOptions = getUiOptions<T, S, F>(uiSchema)
  const WrapIfAdditionalTemplate = getTemplate<'WrapIfAdditionalTemplate', T, S, F>(
    'WrapIfAdditionalTemplate',
    registry,
    uiOptions
  )

  if (hidden) {
    return <div style={{ display: 'none' }}>{children}</div>
  }

  return (
    <WrapIfAdditionalTemplate
      classNames={classNames}
      style={style}
      disabled={disabled}
      id={id}
      label={label}
      onDropPropertyClick={onDropPropertyClick}
      onKeyChange={onKeyChange}
      readonly={readonly}
      required={required}
      schema={schema}
      uiSchema={uiSchema}
      registry={registry}
    >
      <FormControl isRequired={required} isInvalid={rawErrors && rawErrors.length > 0} mb={4}>
        {children}
        {displayLabel && rawDescription && !rawErrors.length ? (
          <Box mt={2} mb={4}>
            {description}
          </Box>
        ) : null}
        {!!rawErrors.length && errors}
        {help}
      </FormControl>
    </WrapIfAdditionalTemplate>
  )
}

export const ReactFlowSchemaForm: FC<Omit<FormProps, 'validator' | 'templates'>> = (props) => {
  return (
    <Form
      showErrorList="bottom"
      templates={{ DescriptionFieldTemplate, FieldTemplate }}
      validator={validator}
      {...props}
    />
  )
}
