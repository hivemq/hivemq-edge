import { FC } from 'react'
import { Circle, Flex, Heading, Image, Text, type HTMLChakraProps } from '@chakra-ui/react'
import DefaultLogo from '@/assets/app/bridge-empty.svg'

interface WarningMessageProps extends HTMLChakraProps<'div'> {
  title?: string
  image?: string | undefined
  prompt: string
  alt: string
}

const WarningMessage: FC<WarningMessageProps> = ({ image = DefaultLogo, prompt, alt, title, ...rest }) => {
  return (
    <Flex flexDirection={'column'} alignItems={'center'} gap={4} {...rest}>
      {title && (
        <Heading as={'h2'} size="md" color={'gray.500'}>
          {title}
        </Heading>
      )}
      <Circle size="335" bg="gray.100">
        <Image objectFit="cover" src={image} alt={alt} />
      </Circle>
      <Text align={'center'} maxW={'400'} color={'gray.600'}>
        {prompt}
      </Text>
    </Flex>
  )
}

export default WarningMessage
