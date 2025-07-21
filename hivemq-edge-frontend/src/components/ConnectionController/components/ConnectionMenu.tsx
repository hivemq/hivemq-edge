import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Icon, MenuItem } from '@chakra-ui/react'
import { MdPlayArrow, MdRestartAlt, MdStop } from 'react-icons/md'

import { StatusTransitionCommand } from '@/api/__generated__'
import type { ConnectionElementProps } from '@/components/ConnectionController/types.ts'

const ConnectionMenu: FC<ConnectionElementProps> = ({ id, isRunning, isLoading, onChangeStatus }) => {
  const { t } = useTranslation()

  return (
    <>
      {!isRunning && (
        <MenuItem
          isDisabled={isLoading}
          data-testid="device-action-start"
          onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.START)}
          icon={<Icon as={MdPlayArrow} boxSize={4} aria-label={t('action.start')} />}
        >
          {t('action.start')}
        </MenuItem>
      )}

      {isRunning && (
        <MenuItem
          isDisabled={isLoading}
          data-testid="device-action-stop"
          onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.STOP)}
          icon={<Icon as={MdStop} boxSize={4} aria-label={t('action.stop')} />}
        >
          {t('action.stop')}
        </MenuItem>
      )}

      <MenuItem
        isDisabled={isLoading || !isRunning}
        data-testid="device-action-restart"
        onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.RESTART)}
        icon={<Icon as={MdRestartAlt} boxSize={4} aria-label={t('action.restart')} />}
      >
        {t('action.restart')}
      </MenuItem>
    </>
  )
}

export default ConnectionMenu
