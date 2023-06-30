import { FC } from 'react'
import { Box, Button, Heading, SimpleGrid, Stack, StackDivider } from '@chakra-ui/react'

import { Card, CardHeader, CardBody, Text } from '@chakra-ui/react'
import { Link as RouterLink } from 'react-router-dom'
import { useOnboarding } from '@/modules/Welcome/hooks/useOnboarding.tsx'
import { useTranslation } from 'react-i18next'

const Onboarding: FC = () => {
  const { t } = useTranslation()
  const content = useOnboarding()
  return (
    <Box mt={6}>
      <Heading>{t('welcome.onboarding.title')}</Heading>
      <SimpleGrid spacing={6} templateColumns="repeat(auto-fill, minmax(30vw, 10fr))">
        {content.map((e) => (
          <Card flex={1} key={e.header}>
            <CardHeader>
              <Heading size="md">{e.header}</Heading>
            </CardHeader>

            <CardBody>
              <Stack divider={<StackDivider />} spacing="4">
                {e.sections.map((s) => (
                  <Box key={s.title}>
                    <Text pt="2" fontSize="sm">
                      {s.title}
                    </Text>
                    <Button
                      variant="link"
                      as={RouterLink}
                      to={s.to}
                      aria-label={s.label}
                      leftIcon={s.leftIcon}
                      size="lg"
                    >
                      {s.label}
                    </Button>
                  </Box>
                ))}
              </Stack>
            </CardBody>
          </Card>
        ))}
      </SimpleGrid>
    </Box>
  )
}

export default Onboarding
