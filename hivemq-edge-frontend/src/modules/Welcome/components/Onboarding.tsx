import type { FC } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { BoxProps } from '@chakra-ui/react'
import {
  Box,
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
import type { OnboardingTask } from '@/modules/Welcome/types.ts'

interface OnboardingProps extends BoxProps {
  tasks: OnboardingTask[]
}

const Onboarding: FC<OnboardingProps> = ({ tasks, ...props }) => {
  const { t } = useTranslation()

  return (
    <Box mt={6} {...props}>
      <Heading>{t('welcome.onboarding.title')}</Heading>
      <SimpleGrid spacing={6} templateColumns="repeat(auto-fill, minmax(33vw, 10fr))">
        {tasks?.map((task, index) => (
          <Card flex={1} key={task.header} as="aside" aria-labelledby={`heading-task-${index}`}>
            <CardHeader>
              <Skeleton isLoaded={!task.isLoading}>
                <Heading as="h3" size="md" id={`heading-task-${index}`}>
                  {task.header}
                </Heading>
              </Skeleton>
            </CardHeader>

            <CardBody pt={0}>
              <Stack divider={<StackDivider />} spacing="4">
                {task.error && (
                  <Card flex={1} m={1}>
                    <ErrorMessage type={task.error?.message} message={t('welcome.onboarding.errorLoading')} />
                  </Card>
                )}
                {!task.error &&
                  task.sections.map((section) => (
                    <Stack as="section" key={`${section.title}-${section.label}`} spacing={8} direction="row" gap={4}>
                      <Skeleton isLoaded={!task.isLoading}>
                        <Box>
                          <BsClipboardCheck />
                        </Box>
                      </Skeleton>
                      <Skeleton isLoaded={!task.isLoading}>
                        <Box>
                          <Text>{section.title}</Text>
                          {!!section.to && (
                            <Button
                              variant="link"
                              as={RouterLink}
                              to={section.to}
                              target={section.isExternal ? '_blank' : undefined}
                              aria-label={section.label}
                              leftIcon={section.leftIcon}
                            >
                              {section.label}
                            </Button>
                          )}
                          {!!section.content && section.content}
                        </Box>
                      </Skeleton>
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
