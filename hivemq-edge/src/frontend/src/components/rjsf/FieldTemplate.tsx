import type { FC } from 'react'
import type { FieldTemplateProps } from '@rjsf/utils'
import { getTemplate, getUiOptions } from '@rjsf/utils'

import { FormControl, FormHelperText } from '@chakra-ui/react'

/**
 * This is a redesign of the original ChakraUI template for fields.
 * See https://github.com/rjsf-team/react-jsonschema-form/blob/main/packages/chakra-ui/src/FieldTemplate/FieldTemplate.tsx
 *
 * @changelog:
 *  - use FormErrorMessage and FormHelperText appropriately
 *  - both help text and error message are mutually exclusive
 */
export const FieldTemplate: FC<FieldTemplateProps> = (props) => {
  const {
    id,
    children,
    classNames,
    style,
    description,
    disabled,
    displayLabel,
    hidden,
    label,
    onDropPropertyClick,
    onKeyChange,
    readonly,
    registry,
    required,
    rawDescription,
    rawErrors,
    schema,
    uiSchema,
    errors,
    help,
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
      <FormControl
        data-testid={id}
        id={id}
        variant="hivemq"
        isRequired={required}
        isInvalid={rawErrors && rawErrors.length > 0}
      >
        {children}
        {rawErrors && rawErrors.length > 0 ? (
          errors
        ) : (
          <FormHelperText mt={4} mb={0}>
            {displayLabel && rawDescription ? description : null}
          </FormHelperText>
        )}
        {help}
      </FormControl>
    </WrapIfAdditionalTemplate>
  )
}
