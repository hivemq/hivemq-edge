import { FC } from 'react'
import { BridgePanelType } from '@/modules/Bridges/types.ts'
import {
  Checkbox,
  Flex,
  FormControl,
  FormErrorMessage,
  FormHelperText,
  FormLabel,
  Input,
  NumberDecrementStepper,
  NumberIncrementStepper,
  NumberInput,
  NumberInputField,
  NumberInputStepper,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'
import { $Bridge } from '@/api/__generated__'

const OptionsPanel: FC<BridgePanelType> = ({ form }) => {
  const { t } = useTranslation()
  const {
    register,
    formState: { errors },
  } = form

  return (
    <Flex flexDirection={'column'} gap={4}>
      <FormControl isInvalid={!!errors.cleanStart}>
        <Checkbox defaultChecked {...register('cleanStart')}>
          {t('bridge.options.cleanStart.label')}
        </Checkbox>
        <FormHelperText> {t('bridge.options.cleanStart.helper')}</FormHelperText>
        <FormErrorMessage>{errors.cleanStart && errors.cleanStart.message}</FormErrorMessage>
      </FormControl>

      <FormControl isInvalid={!!errors.keepAlive}>
        <FormLabel htmlFor="keepAlive">{t('bridge.options.keepAlive.label')}</FormLabel>
        <NumberInput id="keepAlive" step={1} min={0} max={$Bridge.properties.keepAlive.maximum}>
          <NumberInputField
            {...register('keepAlive', {
              required: 'This field is required',
              min: { value: 0, message: 'min should be 0' },
              max: {
                value: $Bridge.properties.keepAlive.maximum,
                message: `max length should be ${$Bridge.properties.keepAlive.maximum}`,
              },
            })}
          />
          <NumberInputStepper>
            <NumberIncrementStepper />
            <NumberDecrementStepper />
          </NumberInputStepper>
        </NumberInput>
        <FormHelperText> {t('bridge.options.keepAlive.helper')}</FormHelperText>
        <FormErrorMessage>{errors.keepAlive && errors.keepAlive.message}</FormErrorMessage>
      </FormControl>

      <FormControl>
        <FormLabel htmlFor="sessionExpiry">{t('bridge.options.sessionExpiry.label')}</FormLabel>
        <NumberInput id="sessionExpiry" step={1} min={1} max={$Bridge.properties.sessionExpiry.maximum}>
          <NumberInputField
            {...register('sessionExpiry', {
              required: 'This field is required',
              min: { value: 0, message: 'min should be 0' },
              max: {
                value: $Bridge.properties.sessionExpiry.maximum,
                message: `max length should be ${$Bridge.properties.sessionExpiry.maximum}`,
              },
            })}
          />
          <NumberInputStepper>
            <NumberIncrementStepper />
            <NumberDecrementStepper />
          </NumberInputStepper>
        </NumberInput>
        <FormHelperText> {t('bridge.options.sessionExpiry.helper')}</FormHelperText>
      </FormControl>

      <FormControl isInvalid={!!errors.loopPreventionEnabled} mt={3}>
        <Checkbox defaultChecked {...register('loopPreventionEnabled')}>
          {t('bridge.options.loopPrevention.label')}
        </Checkbox>
        <FormErrorMessage>{errors.loopPreventionEnabled && errors.loopPreventionEnabled.message}</FormErrorMessage>
      </FormControl>

      <FormControl isInvalid={!!errors.loopPreventionHopCount}>
        <FormLabel htmlFor="loopPreventionHopCount">{t('bridge.options.hopCount.label')}</FormLabel>
        <NumberInput id="loopPreventionHopCount" step={1} min={1} max={100}>
          <NumberInputField
            {...register('loopPreventionHopCount', {
              min: { value: 1, message: 'min should be 1' },
            })}
          />
          <NumberInputStepper>
            <NumberIncrementStepper />
            <NumberDecrementStepper />
          </NumberInputStepper>
        </NumberInput>
        <FormHelperText> {t('bridge.options.hopCount.helper')}</FormHelperText>
        <FormErrorMessage>{errors.loopPreventionHopCount && errors.loopPreventionHopCount.message}</FormErrorMessage>
      </FormControl>

      <FormControl isInvalid={!!errors.clientId}>
        <FormLabel htmlFor="clientId">{t('bridge.connection.clientId')}</FormLabel>
        <Input
          id="clientId"
          type="text"
          {...register('clientId', {
            pattern: {
              value: new RegExp($Bridge.properties.clientId.pattern),
              message: 'Minimum length should be 4',
            },
          })}
        />
        <FormHelperText> {t('bridge.options.clientid.helper')}</FormHelperText>
        <FormErrorMessage>{errors.clientId && errors.clientId.message}</FormErrorMessage>
      </FormControl>
    </Flex>
  )
}

export default OptionsPanel
