import { FC } from 'react'
import { AbsoluteCenter, Box, Circle, Flex, Image, Text } from '@chakra-ui/react'
import DefaultLogo from '@/assets/app/bridge-empty.svg'

interface WarningMessageProps {
  image?: string | undefined
  prompt: string
  alt: string
}

const WarningMessage: FC<WarningMessageProps> = ({ image = DefaultLogo, prompt, alt }) => {
  return (
    <Box w={1} textAlign={'center'}>
      <AbsoluteCenter axis="both">
        <Circle size="500px" bg="gray.100">
          <Flex flexDirection={'column'} alignItems={'center'}>
            <Image objectFit="cover" maxW={{ base: '150px', md: '100%' }} src={image} alt={alt} />
            <Text align={'center'} maxW={'400'}>
              {prompt}
            </Text>
          </Flex>
        </Circle>
      </AbsoluteCenter>
    </Box>
  )
}

export default WarningMessage
