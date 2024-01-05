import { FC } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Box,
  BoxProps,
  Button,
  Heading,
  SimpleGrid,
  Stack,
  StackDivider,
  Card,
  CardHeader,
  CardBody,
  Text,
  Skeleton,
} from '@chakra-ui/react'
import { BsClipboardCheck } from 'react-icons/bs'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import { OnboardingFetchType } from '@/modules/Welcome/hooks/useOnboarding.tsx'

interface OnboardingProps extends BoxProps {
  tasks: OnboardingFetchType
}

const Onboarding: FC<OnboardingProps> = ({ tasks, ...props }) => {
  const { t } = useTranslation()
  const { data, error } = tasks

  return (
    <Box mt={6} {...props}>
      <Heading>{t('welcome.onboarding.title')}</Heading>
      <SimpleGrid spacing={6} templateColumns="repeat(auto-fill, minmax(33vw, 10fr))">
        {data &&
          data.map((e, i) => (
            <Card flex={1} key={e.header} as={'aside'} aria-labelledby={`heading-task-${i}`}>
              <CardHeader>
                <Skeleton isLoaded={!e.isLoading}>
                  <Heading as="h3" size="md" id={`heading-task-${i}`}>
                    {e.header}
                  </Heading>
                </Skeleton>
              </CardHeader>

              <CardBody pt={0}>
                <Stack divider={<StackDivider />} spacing="4">
                  {e.sections.map((s) => (
                    <Stack as="section" key={s.title} spacing={8} direction="row" gap={4}>
                      <Skeleton isLoaded={!e.isLoading}>
                        <Box>
                          <BsClipboardCheck />
                        </Box>
                      </Skeleton>
                      <Skeleton isLoaded={!e.isLoading}>
                        <Box>
                          <Text>{s.title}</Text>
                          <Button
                            variant="link"
                            as={RouterLink}
                            to={s.to}
                            target={s.isExternal ? '_blank' : undefined}
                            aria-label={s.label}
                            leftIcon={s.leftIcon}
                          >
                            {s.label}
                          </Button>
                        </Box>
                      </Skeleton>
                    </Stack>
                  ))}
                </Stack>
              </CardBody>
            </Card>
          ))}
        {!!error && (
          <Card flex={1} m={1}>
            <ErrorMessage type={error?.message} message={t('welcome.onboarding.errorLoading') as string} />
          </Card>
        )}
      </SimpleGrid>
    </Box>
  )
}

export default Onboarding
