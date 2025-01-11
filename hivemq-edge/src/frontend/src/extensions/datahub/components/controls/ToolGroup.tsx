import type { FC, ReactNode } from 'react'
import { ButtonGroup, type ButtonGroupProps, Heading, HStack, VStack } from '@chakra-ui/react'

interface ToolProps extends ButtonGroupProps {
  title: string
  id: string
  children: ReactNode
}

const ToolGroup: FC<ToolProps> = ({ title, id, children, ...props }) => {
  return (
    <ButtonGroup variant="outline" size="sm" aria-labelledby={id} {...props}>
      <VStack alignItems="flex-start">
        <Heading as="h2" size="sm" id={id}>
          {title}
        </Heading>
        <HStack>{children}</HStack>
      </VStack>
    </ButtonGroup>
  )
}

export default ToolGroup
