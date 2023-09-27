import { FC } from 'react'
import { ButtonGroup, IconButton } from '@chakra-ui/react'
import { MdRestartAlt, MdStart, MdStop } from 'react-icons/md'
import { StatusTransitionCommand } from '@/api/__generated__'
import { ConnectionElementProps } from '@/components/ConnectionController/types.ts'
import { useTranslation } from 'react-i18next'

const ConnectionButton: FC<ConnectionElementProps> = ({ id, isRunning, onChangeStatus }) => {
  const { t } = useTranslation()

  return (
    <ButtonGroup size="sm" isAttached variant="outline">
      {!isRunning && (
        <IconButton
          data-testid={'device-action-start'}
          aria-label={t('action.start')}
          icon={<MdStart />}
          onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.START)}
        />
      )}
      {isRunning && (
        <IconButton
          data-testid={'device-action-stop'}
          aria-label={t('action.stop')}
          icon={<MdStop />}
          onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.STOP)}
        />
      )}
      <IconButton
        data-testid={'device-action-restart'}
        aria-label={t('action.restart')}
        icon={<MdRestartAlt />}
        onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.RESTART)}
      />
    </ButtonGroup>
  )
}

export default ConnectionButton
