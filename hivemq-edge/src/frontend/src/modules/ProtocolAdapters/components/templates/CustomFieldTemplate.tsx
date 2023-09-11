import { FC } from 'react'
import { FieldTemplateProps, getTemplate, getUiOptions } from '@rjsf/utils'
import { FormControl, FormErrorMessage, FormHelperText } from '@chakra-ui/react'

/**
 * This is a redesign of the original ChakraUI template for fields.
 * See https://github.com/rjsf-team/react-jsonschema-form/blob/main/packages/chakra-ui/src/FieldTemplate/FieldTemplate.tsx
 *
 * @changelog:
 *  - use FormErrorMessage and FormHelperText appropriately
 *  - both help text and error message are mutually exclusive
 */
const CustomFieldTemplate: FC<FieldTemplateProps> = (props) => {
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

  const uiOptions = getUiOptions(uiSchema)
  const WrapIfAdditionalTemplate = getTemplate<'WrapIfAdditionalTemplate'>(
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
      <FormControl isRequired={required} isInvalid={rawErrors && rawErrors.length > 0}>
        {children}
        {rawErrors && rawErrors.length > 0 ? (
          <FormErrorMessage mt={0}>{errors}</FormErrorMessage>
        ) : (
          <FormHelperText>{displayLabel && rawDescription ? description : null}</FormHelperText>
        )}
        {help}
      </FormControl>
    </WrapIfAdditionalTemplate>
  )
}

export default CustomFieldTemplate
