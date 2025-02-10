import type { FC } from 'react'
import Form from '@rjsf/chakra-ui'
import type { FormProps } from '@rjsf/core'
import type {
  DescriptionFieldProps,
  FieldTemplateProps,
  FormContextType,
  RJSFSchema,
  StrictRJSFSchema,
  ErrorListProps,
  TitleFieldProps,
} from '@rjsf/utils'
import { getTemplate, getUiOptions, TranslatableString } from '@rjsf/utils'
import type { GenericObjectType } from '@rjsf/utils/src/types.ts'
import validator from '@rjsf/validator-ajv8'
import { Alert, AlertTitle, Box, Divider, FormControl, Heading, List, ListIcon, ListItem, Text } from '@chakra-ui/react'
import { WarningIcon } from '@chakra-ui/icons'

import { ArrayFieldItemTemplate } from '@/components/rjsf/ArrayFieldItemTemplate.tsx'
import { ArrayFieldTemplate } from '@/components/rjsf/ArrayFieldTemplate.tsx'

// overriding the heading definition
function TitleFieldTemplate<T = unknown, S extends StrictRJSFSchema = RJSFSchema>({
  id,
  title,
}: TitleFieldProps<T, S>) {
  return (
    <Box id={id} mt={1} mb={4}>
      <Heading as="h2" size="lg">
        {title}
      </Heading>
      <Divider />
    </Box>
  )
}

function ErrorListTemplate<T = unknown, S extends StrictRJSFSchema = RJSFSchema>({
  errors,
  registry,
}: ErrorListProps<T, S>) {
  const { translateString } = registry
  return (
    <Alert flexDirection="column" alignItems="flex-start" gap={3} status="error" mt={4}>
      <AlertTitle>{translateString(TranslatableString.ErrorsLabel)}</AlertTitle>
      <List>
        {errors.map((error, i) => (
          <ListItem key={i}>
            <ListIcon as={WarningIcon} color="red.500" />
            {error.stack}
          </ListItem>
        ))}
      </List>
    </Alert>
  )
}

// Override to fix bug with nested p
function DescriptionFieldTemplate<
  T = unknown,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = GenericObjectType,
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
function FieldTemplate<
  T = unknown,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = GenericObjectType,
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

  // Change the type of wrapper based on existence of a label (indicating a field)
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
        variant={props.displayLabel ? 'hivemq' : undefined}
        isRequired={required}
        isInvalid={rawErrors && rawErrors.length > 0}
        mb={4}
      >
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

interface ReactFlowSchemaFormProps
  extends Omit<FormProps, 'validator' | 'templates' | 'liveValidate' | 'omitExtraData'> {
  isNodeEditable?: boolean
}

export const ReactFlowSchemaForm: FC<ReactFlowSchemaFormProps> = (props) => {
  const { uiSchema, isNodeEditable = true, ...rest } = props

  return (
    <Form
      readonly={!isNodeEditable}
      id="datahub-node-form"
      showErrorList="bottom"
      templates={{
        ArrayFieldItemTemplate,
        ArrayFieldTemplate,
        DescriptionFieldTemplate,
        FieldTemplate,
        ErrorListTemplate,
        TitleFieldTemplate,
      }}
      validator={validator}
      // TODO[NVL] Not sure we want to hide the validation when readonly
      noValidate={!isNodeEditable}
      uiSchema={{
        ...uiSchema,
        'ui:submitButtonOptions': {
          norender: true,
        },
      }}
      liveValidate
      omitExtraData
      {...rest}
    />
  )
}
