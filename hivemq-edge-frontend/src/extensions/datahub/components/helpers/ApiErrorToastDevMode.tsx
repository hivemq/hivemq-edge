import type { FC } from 'react'
import { Button, ListItem, Text, UnorderedList, useDisclosure, VStack } from '@chakra-ui/react'
import type { ProblemDetails } from '@/api/__generated__'

interface ApiErrorToastDevModeProps {
  message: string
  body?: ProblemDetails
}

/**
 * A dev-only wrapper for error messages that are not properly formatted as Problem Details
 * @todo DO NOT USE IN PRODUCTION
 */
const ApiErrorToastDevMode: FC<ApiErrorToastDevModeProps> = ({ message, body }) => {
  const show = useDisclosure()
  return (
    <VStack alignItems="self-start">
      <Text>{message}</Text>
      {body && (
        <>
          <Button
            data-testid="content-toggle"
            variant="link"
            size="xs"
            onClick={() => show.onToggle()}
            color="var(--alert-fg)"
          >
            show more (dev only)
          </Button>
          {show.isOpen && (
            <UnorderedList fontSize="sm">
              <ListItem>
                <Text>{body.detail}</Text>
              </ListItem>
              {body.errors?.map((error, index) => (
                <ListItem key={`key-${index}`}>
                  <Text>
                    {error.detail} - {error.parameter}
                  </Text>
                </ListItem>
              ))}
            </UnorderedList>
          )}
        </>
      )}
    </VStack>
  )
}

export default ApiErrorToastDevMode
