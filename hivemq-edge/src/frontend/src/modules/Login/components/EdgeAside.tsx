import { FC } from 'react'
import { Box, Heading, Image, Stack, Text } from '@chakra-ui/react'
import logo from '@/assets/edge/04-hivemq-industrial-edge-vert-neg.svg'
import { useTranslation } from 'react-i18next'

const EdgeAside: FC = () => {
  const { t } = useTranslation()

  return (
    <aside>
      <Stack
        direction={{ base: 'row', md: 'column' }}
        align={{ base: 'center', md: 'flex-start' }}
        m={{ base: '1', md: '6' }}
      >
        <Box>
          <Heading
            textAlign={{ base: 'center', md: 'left' }}
            pt={{ base: '0', md: '20' }}
            color={'white'}
            lineHeight={1.1}
            display={{ base: 'none', md: 'inherit' }}
            fontSize={{ md: '6xl', lg: '7xl' }}
          >
            {t('branding.appName')}
          </Heading>
          <Image
            src={logo}
            alt={'branding.appName' as string}
            boxSize={{ base: '120px', md: '500px' }}
            display={{ base: 'inherit', md: 'none' }}
          />
        </Box>
        <Box>
          <Box>
            <Heading
              textAlign={{ base: 'center', md: 'left' }}
              pt={{ base: '0', md: '20' }}
              color={'white'}
              lineHeight={1.1}
              fontSize={{ base: 'md', sm: '2xl', md: '4xl', lg: '5xl' }}
            >
              {t('login.aside.header')}
            </Heading>
            <Text
              fontSize={{ base: 'md', sm: '2xl', md: '4xl' }}
              color={'white'}
              textAlign={{ base: 'center', md: 'left' }}
            >
              {t('login.aside.subheader')}
            </Text>
          </Box>
          <Text
            fontSize={{ base: 'sm', md: 'lg' }}
            color={'white'}
            textAlign={{ base: 'center', md: 'left' }}
            mt={{ base: 0, md: 10 }}
          >
            {t('login.aside.tagline')}
          </Text>
        </Box>
      </Stack>
    </aside>
  )
}

export default EdgeAside
