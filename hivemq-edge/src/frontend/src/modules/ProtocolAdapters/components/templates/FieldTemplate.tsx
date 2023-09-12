import { FC } from 'react'
import { FieldTemplateProps, getTemplate, getUiOptions } from '@rjsf/utils'

import { RenderFieldTemplate } from './__internals/RenderFieldTemplate.tsx'

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
    disabled,
    hidden,
    label,
    onDropPropertyClick,
    onKeyChange,
    readonly,
    registry,
    required,
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
      <RenderFieldTemplate {...props} children={children} />
    </WrapIfAdditionalTemplate>
  )
}
