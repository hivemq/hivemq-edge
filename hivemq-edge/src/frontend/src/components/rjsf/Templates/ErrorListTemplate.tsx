import type { FC } from 'react'
import { useCallback, useMemo } from 'react'
import type { ErrorListProps, RJSFSchema } from '@rjsf/utils'
import { TranslatableString } from '@rjsf/utils'
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
import type { RJSFValidationError } from '@rjsf/utils/src/types.ts'

import type { ChakraRJSFormContext, UITabIndexed } from '@/components/rjsf/Form/types.ts'
import { useTranslation } from 'react-i18next'
import { useFormControlStore } from '@/components/rjsf/Form/useFormControlStore.ts'
import { isPropertyBehindCollapsedElement, isPropertyBehindTab } from '@/components/rjsf/Form/error-focus.utils.ts'

interface RJSFValidationErrorRef extends RJSFValidationError {
  tab?: UITabIndexed
  collapsed?: string[]
}

export const ErrorListTemplate: FC<ErrorListProps<unknown, RJSFSchema, ChakraRJSFormContext>> = (props) => {
  const { uiSchema, errors, registry, formContext } = props
  const { t } = useTranslation('components')
  const { setTabIndex, setExpandItems } = useFormControlStore()

  const linkedErrors = useMemo(() => {
    if (!uiSchema) return errors as RJSFValidationErrorRef[]
    return errors.map<RJSFValidationErrorRef>((error) => {
      const { property } = error
      if (!property) return error

      let newError: RJSFValidationErrorRef = { ...error }

      const collapsedPath = isPropertyBehindCollapsedElement(property, uiSchema)
      if (collapsedPath) {
        newError = { ...newError, collapsed: collapsedPath }
      }

      const tabbedItem = isPropertyBehindTab(property, uiSchema)
      if (tabbedItem) newError = { ...newError, tab: tabbedItem }

      return newError
    })
  }, [errors, uiSchema])

  const handleShiftFocus = useCallback(
    (error: RJSFValidationErrorRef) => () => {
      const isTab = Boolean(error.tab?.index !== undefined)
      const isAccordion = Boolean(error.collapsed)

      // must be done in order of DOM nesting: tab, collapse, focus
      if (error.tab && isTab) setTabIndex(error.tab.index)
      if (error.collapsed) setExpandItems(error.collapsed)
      // TODO[NVL] Scroll animation would be much better
      setTimeout(
        () => {
          formContext?.focusOnError?.(error)
        },
        isTab || isAccordion ? 100 : 0
      )
    },
    [setTabIndex, setExpandItems, formContext]
  )

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
                    <Button colorScheme="red" variant="link" color="red.700" onClick={handleShiftFocus(error)}>
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
                  onClick={handleShiftFocus(error)}
                />
              </Box>
            </HStack>
          </ListItem>
        ))}
      </List>
    </Alert>
  )
}
