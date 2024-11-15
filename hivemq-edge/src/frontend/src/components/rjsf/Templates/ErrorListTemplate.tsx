import { FC, useMemo } from 'react'
import { ErrorListProps, RJSFSchema, TranslatableString, UiSchema } from '@rjsf/utils'
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  HStack,
  Icon,
  IconButton,
  List,
  ListIcon,
  ListItem,
  Text,
} from '@chakra-ui/react'
import { WarningIcon } from '@chakra-ui/icons'
import { IoLink } from 'react-icons/io5'
import { RJSFValidationError } from '@rjsf/utils/src/types.ts'

import { AdapterConfig, UITab } from '@/modules/ProtocolAdapters/types.ts'
import { ChakraRJSFormContext } from '@/components/rjsf/Form/types.ts'
import { useTranslation } from 'react-i18next'

interface RJSFValidationErrorRef extends RJSFValidationError {
  tab?: UITab
}

export const ErrorListTemplate: FC<ErrorListProps<unknown, RJSFSchema, ChakraRJSFormContext>> = (props) => {
  const { t } = useTranslation('components')
  const { uiSchema, errors, registry, formContext } = props

  const linkedErrors = useMemo(() => {
    return errors.map<RJSFValidationErrorRef>((error) => {
      const { 'ui:tabs': tabs } = uiSchema as UiSchema<AdapterConfig>
      const { property } = error
      if (!tabs) return error

      const root = property?.split('.')[1] || property

      let inTab: UITab | null = null
      for (const tab of tabs) {
        const { properties } = tab as UITab
        if (properties && root && properties.includes(root)) inTab = tab
      }

      if (inTab) return { ...error, tab: inTab }
      return error
    })
  }, [errors, uiSchema])

  const { translateString } = registry

  return (
    <Alert flexDirection="column" alignItems="flex-start" gap={3} status="error" mt={4}>
      <AlertTitle>{translateString(TranslatableString.ErrorsLabel)}</AlertTitle>
      <List>
        {linkedErrors.map((error, i) => (
          <ListItem key={i}>
            <HStack>
              <ListIcon as={WarningIcon} color="red.500" />
              <Box>
                {error.tab && (
                  <>
                    <Button
                      colorScheme="red"
                      variant="link"
                      color="red.700"
                      onClick={() => formContext?.focusOnError?.(error)}
                    >
                      {error.tab?.title}
                    </Button>{' '}
                  </>
                )}
                <Text as="span">{error.stack}</Text>{' '}
                <IconButton
                  icon={<Icon as={IoLink} />}
                  variant="link"
                  aria-label={t('rjsf.ErrorListTemplate.focusOnError.aria-label')}
                  color="red.700"
                  size="sm"
                  onClick={() => formContext?.focusOnError?.(error)}
                />
              </Box>
            </HStack>
          </ListItem>
        ))}
      </List>
    </Alert>
  )
}
