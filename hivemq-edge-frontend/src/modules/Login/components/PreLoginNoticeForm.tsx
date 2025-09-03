import type { FC, UIEvent } from 'react'
import { useCallback, useState } from 'react'

import { Box, Button, Flex, FormControl, Heading, Checkbox, HStack, FormErrorMessage, Textarea } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import type { PreLoginNotice } from '@/api/__generated__'

interface PreLoginNoticeFormProps {
  notice: PreLoginNotice
  forceReading?: boolean
  onAccept: () => void
}

const PreLoginNoticeForm: FC<PreLoginNoticeFormProps> = ({ notice, onAccept, forceReading = false }) => {
  const { t } = useTranslation()
  const [isConsentChecked, setIsConsentChecked] = useState(!notice.consent)
  const [isStatementRead, setIsStatementRead] = useState(false)

  const onScroll = useCallback((e: UIEvent<HTMLTextAreaElement>) => {
    const el = e.currentTarget
    const tolerance = 2 // px to account for rounding
    const reachedBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - tolerance
    if (reachedBottom) setIsStatementRead(reachedBottom)
  }, [])

  return (
    <Flex align="center" flexDirection="column">
      <Box width="100%" maxWidth="450px" p={3} id="confidentiality">
        <Heading as="h1" mb={6}>
          {notice.title}
        </Heading>
      </Box>

      <Box p={4} width="100%" maxWidth="450px">
        <form>
          <Textarea
            data-testid="confidentiality-form-content"
            aria-labelledby="confidentiality"
            onScroll={onScroll}
            textAlign="justify"
            fontWeight="larger"
            resize="none"
            isReadOnly
            h="25vh"
          >
            {notice.message}
          </Textarea>

          {notice.consent && (
            <FormControl isRequired mt={12} isInvalid={forceReading && !isStatementRead}>
              <Checkbox
                isChecked={isConsentChecked}
                isDisabled={forceReading && !isStatementRead}
                onChange={() => setIsConsentChecked(!isConsentChecked)}
                isRequired
                data-testid="confidentiality-form-agreement"
              >
                {notice.consent}
              </Checkbox>
              {forceReading && !isStatementRead && (
                <FormErrorMessage>{t('login.preLogin.forceReading')}</FormErrorMessage>
              )}
            </FormControl>
          )}

          <HStack width="100%" spacing={4} mt="7em" justifyContent="flex-end">
            <Button
              data-testid="confidentiality-form-submit"
              width="50%"
              variant="primary"
              isDisabled={!isConsentChecked}
              onClick={onAccept}
            >
              {t('Proceed to login')}
            </Button>
          </HStack>
        </form>
      </Box>
    </Flex>
  )
}

export default PreLoginNoticeForm
