import { Link as RouterLink, useRouteError } from 'react-router-dom'
import { Heading, VStack, Text, Button } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { FaHome } from 'react-icons/fa'

interface ErrorResponse {
  status: number
  statusText: string
  data: never
  error?: Error
  internal: boolean
}

const ErrorPage = () => {
  const error = useRouteError() as ErrorResponse
  const { t } = useTranslation()

  return (
    <main>
      <VStack justify="center" spacing="4" as="section" mt={['20', null, '40']} textAlign="center">
        <Heading as="h1">{t('error.notfound.heading')}</Heading>
        <Text fontSize={{ md: 'xl' }}>
          {error.statusText} | {error.status}
        </Text>
        <Button as={RouterLink} to={'/'} aria-label="Back to Home" leftIcon={<FaHome />} size="lg">
          {t('error.backHome')}
        </Button>
      </VStack>
      {import.meta.env.MODE === 'development' && (
        <VStack m={8} maxH={'250px'} mt={20} p={2} overflow={'auto'} bgColor={'gray.100'}>
          <pre style={{ width: '100%' }}>{error.error?.stack}</pre>{' '}
        </VStack>
      )}
    </main>
  )
}
export default ErrorPage
