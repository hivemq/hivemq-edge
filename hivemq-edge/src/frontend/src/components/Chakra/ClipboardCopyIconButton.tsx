import { FC, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Icon, IconButton, IconButtonProps, Tooltip, PlacementWithLogical } from '@chakra-ui/react'
import { LuClipboardCopy } from 'react-icons/lu'
import { VscCheck, VscError } from 'react-icons/vsc'

enum CopyStatus {
  READY = 'READY',
  COPIED = 'COPIED',
  ERROR = 'ERROR',
}

interface ClipboardCopyIconButtonProps extends Omit<IconButtonProps, 'aria-label'> {
  content: string
  placement?: PlacementWithLogical
}

const COPY_LATENCY = 1500

const ClipboardCopyIconButton: FC<ClipboardCopyIconButtonProps> = ({ content, placement, ...props }) => {
  const { t } = useTranslation('components')
  const [state, setState] = useState<CopyStatus>(CopyStatus.READY)

  useEffect(() => {
    if (state === CopyStatus.READY) return

    const interval = setInterval(() => {
      setState(CopyStatus.READY)
    }, COPY_LATENCY)
    return () => clearInterval(interval)
  }, [setState, state])

  function handleClick() {
    navigator.clipboard.writeText(content).then(
      () => {
        setState(CopyStatus.COPIED)
      },
      () => {
        setState(CopyStatus.ERROR)
      }
    )
    setState(CopyStatus.COPIED)
  }

  return (
    <Tooltip
      label={t('ClipboardCopyIconButton.action', { context: state })}
      hasArrow
      placement={placement || 'right'}
      isOpen={state !== CopyStatus.READY}
    >
      <IconButton
        isLoading={state != CopyStatus.READY}
        data-state={state}
        spinner={
          state === CopyStatus.ERROR ? (
            <Icon as={VscError} fontSize={'1rem'} color="red.500" />
          ) : (
            <Icon as={VscCheck} fontSize={'1rem'} color="green.500" />
          )
        }
        data-testid="metrics-copy"
        size={'xs'}
        variant={'ghost'}
        icon={<Icon as={LuClipboardCopy} fontSize={'1rem'} />}
        onClick={handleClick}
        aria-label={t('ClipboardCopyIconButton.ariaLabel')}
        {...props}
      />
    </Tooltip>
  )
}

export default ClipboardCopyIconButton
