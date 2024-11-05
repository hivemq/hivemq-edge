import { ErrorListProps, RJSFSchema, StrictRJSFSchema, TranslatableString } from '@rjsf/utils'
import { Alert, AlertTitle, List, ListIcon, ListItem } from '@chakra-ui/react'
import { WarningIcon } from '@chakra-ui/icons'

export const ErrorListTemplate = <T = unknown, S extends StrictRJSFSchema = RJSFSchema>({
  errors,
  registry,
}: ErrorListProps<T, S>) => {
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
