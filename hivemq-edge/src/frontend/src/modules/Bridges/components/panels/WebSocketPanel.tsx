import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Checkbox, FormControl, FormErrorMessage, FormHelperText, FormLabel, Input, Text } from '@chakra-ui/react'

import type { BridgePanelType } from '../../types.ts'

const WebSocketPanel: FC<BridgePanelType> = ({ form }) => {
  const { t } = useTranslation()
  const {
    register,
    formState: { errors },
  } = form

  return (
    <FormControl variant="hivemq" flexGrow={1} display="flex" flexDirection="column" gap={4} as="fieldset">
      <Text>{t('bridge.websocket.description')}</Text>
      <FormControl isInvalid={!!errors.websocketConfiguration?.enabled}>
        <Checkbox {...register('websocketConfiguration.enabled')} data-testid="form-websocket-enabled">
          {t('bridge.websocket.enabled.label')}
        </Checkbox>
        <FormHelperText>{t('bridge.websocket.enabled.helper')}</FormHelperText>
        <FormErrorMessage>
          {errors.websocketConfiguration?.enabled && errors.websocketConfiguration.enabled.message}
        </FormErrorMessage>
      </FormControl>

      <FormControl as="fieldset" variant="hivemq">
        <FormLabel htmlFor="serverPath">{t('bridge.websocket.serverPath.label')}</FormLabel>
        <Input
          data-testid="form-websocket-serverPath"
          autoFocus
          id="serverPath"
          type="text"
          autoComplete="on"
          defaultValue="/mqtt"
          {...register('websocketConfiguration.serverPath')}
        />
        <FormHelperText>{t('bridge.websocket.serverPath.helper')}</FormHelperText>
      </FormControl>

      <FormControl as="fieldset" variant="hivemq">
        <FormLabel htmlFor="subProtocol">{t('bridge.websocket.subProtocol.label')}</FormLabel>
        <Input
          data-testid="form-websocket-subProtocol"
          autoFocus
          id="subProtocol"
          type="text"
          autoComplete="on"
          defaultValue="mqtt"
          {...register('websocketConfiguration.subProtocol')}
        />
        <FormHelperText>{t('bridge.websocket.subProtocol.helper')}</FormHelperText>
      </FormControl>
    </FormControl>
  )
}

export default WebSocketPanel
