import { FC } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Box,
  Button,
  Heading,
  HTMLChakraProps,
  SimpleGrid,
  Stack,
  StackDivider,
  Card,
  CardHeader,
  CardBody,
  Text,
} from '@chakra-ui/react'
import { BsClipboardCheck } from 'react-icons/bs'

import { OnboardingTask } from '@/modules/Welcome/types.ts'

interface OnboardingProps extends HTMLChakraProps<'div'> {
  tasks: OnboardingTask[]
}

const Onboarding: FC<OnboardingProps> = (props) => {
  const { t } = useTranslation()
  const { tasks } = props

  return (
    <Box mt={6} {...props}>
      <Heading>{t('welcome.onboarding.title')}</Heading>
      <SimpleGrid spacing={6} templateColumns="repeat(auto-fill, minmax(33vw, 10fr))">
        {tasks.map((e) => (
          <Card flex={1} key={e.header}>
            <CardHeader>
              <Heading size="md">{e.header}</Heading>
            </CardHeader>

            <CardBody pt={0}>
              <Stack divider={<StackDivider />} spacing="4">
                {e.sections.map((s) => (
                  <Stack key={s.title} spacing={8} direction="row" gap={4}>
                    <Box>
                      <BsClipboardCheck />
                    </Box>
                    <Box>
                      <Text fontSize="sm">{s.title}</Text>
                      <Button
                        variant="link"
                        as={RouterLink}
                        to={s.to}
                        target={s.isExternal ? '_blank' : undefined}
                        aria-label={s.label}
                        leftIcon={s.leftIcon}
                        size="lg"
                      >
                        {s.label}
                      </Button>
                    </Box>
                  </Stack>
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
