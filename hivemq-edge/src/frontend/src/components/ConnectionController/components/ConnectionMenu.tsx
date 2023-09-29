import { FC } from 'react'
import { ConnectionElementProps } from '@/components/ConnectionController/types.ts'
import { MenuItem } from '@chakra-ui/react'
import { StatusTransitionCommand } from '@/api/__generated__'
import { useTranslation } from 'react-i18next'

const ConnectionMenu: FC<ConnectionElementProps> = ({ id, isRunning, isLoading, onChangeStatus }) => {
  const { t } = useTranslation()

  return (
    <>
      {!isRunning && (
        <MenuItem
          isDisabled={isLoading}
          data-testid={'device-action-start'}
          onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.START)}
        >
          {t('action.start')}
        </MenuItem>
      )}

      {isRunning && (
        <MenuItem
          isDisabled={isLoading}
          data-testid={'device-action-stop'}
          onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.STOP)}
        >
          {t('action.stop')}
        </MenuItem>
      )}

      <MenuItem
        isDisabled={isLoading || !isRunning}
        data-testid={'device-action-restart'}
        onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.RESTART)}
      >
        {t('action.restart')}
      </MenuItem>
    </>
  )
}

export default ConnectionMenu
