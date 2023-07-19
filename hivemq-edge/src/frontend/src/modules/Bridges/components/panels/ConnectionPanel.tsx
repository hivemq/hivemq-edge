import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Box,
  Flex,
  FormControl,
  FormErrorMessage,
  FormLabel,
  Input,
  NumberDecrementStepper,
  NumberIncrementStepper,
  NumberInput,
  NumberInputField,
  NumberInputStepper,
} from '@chakra-ui/react'

import { $Bridge } from '@/api/__generated__'
import { useValidationRules } from '@/api/hooks/useValidationRules/useValidationRules.ts'
import { BridgePanelType } from '@/modules/Bridges/types.ts'

const ConnectionPanel: FC<BridgePanelType> = ({ form }) => {
  const { t } = useTranslation()
  const getRulesForProperty = useValidationRules()
  const {
    register,
    formState: { errors },
  } = form

  return (
    <Flex flexDirection={'column'} w={'80%'} m={'auto'} mt={1} mb={4} maxW={600} gap={4}>
      <Flex>
        <Box flexGrow={1}>
          <FormControl isInvalid={!!errors.host} isRequired>
            <FormLabel htmlFor="host">{t('bridge.connection.host')}</FormLabel>
            <Input id="host" type="text" required {...register('host', getRulesForProperty($Bridge.properties.host))} />
            <FormErrorMessage>{errors.host && errors.host.message}</FormErrorMessage>
          </FormControl>
        </Box>
        <Box flexGrow={0}>
          <FormControl isInvalid={!!errors.port} isRequired>
            <FormLabel htmlFor="port">{t('bridge.connection.port')}</FormLabel>
            <NumberInput allowMouseWheel focusInputOnChange w={'100px'} id="port" step={1} min={1}>
              <NumberInputField {...register('port', getRulesForProperty($Bridge.properties.port))} />
              <NumberInputStepper>
                <NumberIncrementStepper />
                <NumberDecrementStepper />
              </NumberInputStepper>
            </NumberInput>
            <FormErrorMessage>{errors.port && errors.port.message}</FormErrorMessage>
          </FormControl>
        </Box>
      </Flex>

      <Flex gap={2}>
        <Box flexGrow={1}>
          <FormControl isInvalid={!!errors.username}>
            <FormLabel htmlFor="username">{t('bridge.connection.username')}</FormLabel>
            <Input
              id="username"
              type="text"
              autoComplete="username"
              {...register('username', getRulesForProperty($Bridge.properties.username))}
            />
            <FormErrorMessage>{errors.username && errors.username.message}</FormErrorMessage>
          </FormControl>
        </Box>
        <Box flexGrow={1}>
          <FormControl isInvalid={!!errors.password}>
            <FormLabel htmlFor="password">{t('bridge.connection.password')}</FormLabel>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              {...register('password', getRulesForProperty($Bridge.properties.password))}
            />
            <FormErrorMessage>{errors.password && errors.password.message}</FormErrorMessage>
          </FormControl>
        </Box>
      </Flex>
      {/*<Flex justifyContent={'flex-end'}>*/}
      {/*  <Button isDisabled size={'sm'} variant="outline" leftIcon={<FaConnectdevelop />}>*/}
      {/*    {t('bridge.connection.testConnection')}*/}
      {/*  </Button>*/}
      {/*</Flex>*/}
    </Flex>
  )
}

export default ConnectionPanel
