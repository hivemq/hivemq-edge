import { FC } from 'react'
import { Box, Heading, Image, Stack, Text } from '@chakra-ui/react'
import logo from '@/assets/edge/04-hivemq-industrial-edge-vert-neg.svg'
import { useTranslation } from 'react-i18next'

const EdgeAside: FC = () => {
  const { t } = useTranslation()

  return (
    <aside>
      <Stack direction={{ base: 'row', md: 'column' }} align={'center'} m={{ base: '1', md: '6' }}>
        <Box>
          <Image src={logo} alt={'branding.company' as string} boxSize={{ base: '150px', md: '500px' }} />
        </Box>
        <Heading
          textAlign={'center'}
          pt={{ base: '0', md: '20' }}
          color={'white'}
          lineHeight={1.1}
          fontSize={{ base: '1xl', sm: '2xl', md: '3xl' }}
        >
          {t('login.aside.header')}
        </Heading>
        <Text color={'white'} fontFamily={'heading'} textAlign={'center'}>
          {t('login.aside.subheader')}
        </Text>
      </Stack>
    </aside>
  )
}

export default EdgeAside
