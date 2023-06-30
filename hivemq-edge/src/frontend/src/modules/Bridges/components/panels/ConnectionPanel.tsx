import { FC } from 'react'
import { BridgePanelType } from '@/modules/Bridges/types.ts'
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
// import { FaConnectdevelop } from 'react-icons/fa'
import { useTranslation } from 'react-i18next'

const ConnectionPanel: FC<BridgePanelType> = ({ form }) => {
  const { t } = useTranslation()
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
            <Input
              id="host"
              type="text"
              required
              {...register('host', {
                required: 'This is required',
                // minLength: { value: 4, message: 'Minimum length should be 4' },
                pattern: { value: /[A-Za-z]{3}/, message: 'Minimum length should be 4' },
              })}
            />
            <FormErrorMessage>{errors.host && errors.host.message}</FormErrorMessage>
          </FormControl>
        </Box>
        <Box flexGrow={0}>
          <FormControl isInvalid={!!errors.port} isRequired>
            <FormLabel htmlFor="port">{t('bridge.connection.port')}</FormLabel>
            <NumberInput allowMouseWheel focusInputOnChange w={'100px'} id="port" step={1} min={1}>
              <NumberInputField
                {...register('port', {
                  required: 'This field is required',
                  min: { value: 10, message: 'min should be 0' },
                  max: {
                    value: $Bridge.properties.port.maximum,
                    message: `max length should be ${$Bridge.properties.port.maximum}`,
                  },
                })}
              />
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
              {...register('username', {
                pattern: {
                  value: new RegExp($Bridge.properties.username.pattern),
                  message: 'Minimum length should be 4',
                },
              })}
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
              {...register('password', {
                pattern: {
                  value: new RegExp($Bridge.properties.password.pattern),
                  message: 'Minimum length should be 4',
                },
              })}
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
