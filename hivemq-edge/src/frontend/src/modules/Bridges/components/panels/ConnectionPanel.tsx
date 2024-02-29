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
    <Flex flexDirection={'column'} m={'auto'} mt={4} mb={4} gap={4}>
      <FormControl variant={'hivemq'} flex={1} display={'flex'} gap={4} as={'fieldset'}>
        <FormControl isInvalid={!!errors.host} isRequired>
          <FormLabel htmlFor="host">{t('bridge.connection.host')}</FormLabel>
          <Input id="host" type="text" required {...register('host', getRulesForProperty($Bridge.properties.host))} />
          <FormErrorMessage>{errors.host && errors.host.message}</FormErrorMessage>
        </FormControl>

        <FormControl isInvalid={!!errors.port} isRequired w={'unset'}>
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
      </FormControl>

      <FormControl variant={'hivemq'} flexGrow={1} display={'flex'} gap={4} as={'fieldset'}>
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
      </FormControl>
    </Flex>
  )
}

export default ConnectionPanel
