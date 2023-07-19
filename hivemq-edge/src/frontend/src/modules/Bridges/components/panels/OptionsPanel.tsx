import { FC } from 'react'
import { useTranslation } from 'react-i18next'
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
import { $Bridge } from '@/api/__generated__'
import { useValidationRules } from '@/api/hooks/useValidationRules/useValidationRules.ts'
import { BridgePanelType } from '@/modules/Bridges/types.ts'

const OptionsPanel: FC<BridgePanelType> = ({ form }) => {
  const { t } = useTranslation()
  const {
    register,
    formState: { errors },
  } = form
  const getRulesForProperty = useValidationRules()

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
        <NumberInput id="keepAlive" step={1}>
          <NumberInputField
            {...register('keepAlive', {
              ...getRulesForProperty($Bridge.properties.keepAlive),
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
        <NumberInput id="sessionExpiry" step={1} max={$Bridge.properties.sessionExpiry.maximum}>
          <NumberInputField
            {...register('sessionExpiry', {
              ...getRulesForProperty($Bridge.properties.sessionExpiry),
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
        <Checkbox
          defaultChecked
          {...register('loopPreventionEnabled', {
            ...getRulesForProperty($Bridge.properties.loopPreventionEnabled),
          })}
        >
          {t('bridge.options.loopPrevention.label')}
        </Checkbox>
        <FormErrorMessage>{errors.loopPreventionEnabled && errors.loopPreventionEnabled.message}</FormErrorMessage>
      </FormControl>

      <FormControl isInvalid={!!errors.loopPreventionHopCount}>
        <FormLabel htmlFor="loopPreventionHopCount">{t('bridge.options.hopCount.label')}</FormLabel>
        <NumberInput id="loopPreventionHopCount" step={1}>
          <NumberInputField
            {...register('loopPreventionHopCount', {
              ...getRulesForProperty($Bridge.properties.loopPreventionHopCount),
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
            ...getRulesForProperty($Bridge.properties.clientId),
          })}
        />
        <FormHelperText> {t('bridge.options.clientid.helper')}</FormHelperText>
        <FormErrorMessage>{errors.clientId && errors.clientId.message}</FormErrorMessage>
      </FormControl>
    </Flex>
  )
}

export default OptionsPanel
