import { FC, ReactNode } from 'react'
import { Box, Text, Flex, Heading, chakra as Chakra } from '@chakra-ui/react'

interface PageContainerProps {
  title: string
  subtitle?: string
  children?: ReactNode
  cta?: ReactNode
}

const PageContainer: FC<PageContainerProps> = ({ title, subtitle, children, cta }) => {
  return (
    <Flex flexDirection="column" p={4} pt={6} flexGrow={1}>
      <Flex gap="50px">
        <Chakra.header maxW="50vw" pb={6} data-testid="page-container-header">
          <Heading as="h1">{title}</Heading>
          {subtitle && <Text fontSize="md">{subtitle}</Text>}
        </Chakra.header>
        <Box flexGrow={1} alignItems="flex-end" data-testid="page-container-cta">
          {cta}
        </Box>
      </Flex>
      {children}
    </Flex>
  )
}

export default PageContainer
