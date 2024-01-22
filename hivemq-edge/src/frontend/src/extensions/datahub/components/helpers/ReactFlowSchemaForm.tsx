import { FC, PropsWithChildren } from 'react'
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
  ErrorListProps,
  TranslatableString,
} from '@rjsf/utils'
import { GenericObjectType } from '@rjsf/utils/src/types.ts'
import validator from '@rjsf/validator-ajv8'
import { Alert, AlertTitle, Box, FormControl, List, ListIcon, ListItem, Text } from '@chakra-ui/react'
import { WarningIcon } from '@chakra-ui/icons'

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
function FieldTemplate<
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

  // Change the type of wrapper based on existence of a label (indicating a field)
  const Wrapper: FC<PropsWithChildren> = ({ children }) => {
    return props.displayLabel ? (
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
        <FormControl variant={'hivemq'} isRequired={required} isInvalid={rawErrors && rawErrors.length > 0} mb={4}>
          {children}
        </FormControl>
      </WrapIfAdditionalTemplate>
    ) : (
      <Box>{children}</Box>
    )
  }

  return (
    <Wrapper>
      <>
        {children}
        {displayLabel && rawDescription && !rawErrors.length ? (
          <Box mt={2} mb={4}>
            {description}
          </Box>
        ) : null}
        {!!rawErrors.length && errors}
        {help}
      </>
    </Wrapper>
  )
}

export const ReactFlowSchemaForm: FC<Omit<FormProps, 'validator' | 'templates' | 'liveValidate' | 'omitExtraData'>> = (
  props
) => {
  const { uiSchema, ...rest } = props
  return (
    <Form
      id="datahub-node-form"
      showErrorList="bottom"
      templates={{ DescriptionFieldTemplate, FieldTemplate, ErrorListTemplate }}
      validator={validator}
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
