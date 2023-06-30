import { FC, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Box, Text, Flex, Heading, VisuallyHidden } from '@chakra-ui/react'

interface PageContainerProps {
  title?: string
  subtitle?: string
  children?: ReactNode
  cta?: ReactNode
}

const PageContainer: FC<PageContainerProps> = ({ title, subtitle, children, cta }) => {
  const { t } = useTranslation()

  return (
    <Flex flexDirection={'column'} p={4} pt={6} flexGrow={1}>
      <Flex gap={'50px'}>
        <Box maxW="50vw" pb={6}>
          <Heading as={'h1'}>
            {title ? (
              title
            ) : (
              <VisuallyHidden>
                <h1>{t('translation:navigation.mainPage')}</h1>
              </VisuallyHidden>
            )}
          </Heading>
          {subtitle && <Text fontSize="md">{subtitle}</Text>}
        </Box>
        <Box flexGrow={1} alignItems={'flex-end'}>
          {cta}
        </Box>
      </Flex>
      {children}
    </Flex>
  )
}

export default PageContainer
