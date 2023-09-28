import { FC } from 'react'
import { ButtonGroup, IconButton } from '@chakra-ui/react'
import { MdPlayArrow, MdRestartAlt, MdStop } from 'react-icons/md'
import { StatusTransitionCommand } from '@/api/__generated__'
import { ConnectionElementProps } from '@/components/ConnectionController/types.ts'
import { useTranslation } from 'react-i18next'

const ConnectionButton: FC<ConnectionElementProps> = ({ id, isRunning, onChangeStatus, isLoading }) => {
  const { t } = useTranslation()

  return (
    <ButtonGroup size="sm" isAttached variant="outline">
      {!isRunning && (
        <IconButton
          isDisabled={isLoading}
          data-testid={'device-action-start'}
          aria-label={t('action.start')}
          icon={<MdPlayArrow />}
          onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.START)}
        />
      )}
      {isRunning && (
        <IconButton
          isDisabled={isLoading}
          data-testid={'device-action-stop'}
          aria-label={t('action.stop')}
          icon={<MdStop />}
          onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.STOP)}
        />
      )}
      <IconButton
        isDisabled={isLoading}
        data-testid={'device-action-restart'}
        aria-label={t('action.restart')}
        icon={<MdRestartAlt />}
        onClick={() => onChangeStatus?.(id, StatusTransitionCommand.command.RESTART)}
      />
    </ButtonGroup>
  )
}

export default ConnectionButton
