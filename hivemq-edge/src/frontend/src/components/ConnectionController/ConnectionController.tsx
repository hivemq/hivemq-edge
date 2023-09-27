import { FC } from 'react'
import { useTranslation } from 'react-i18next'

import { ApiError, Status, StatusTransitionCommand } from '@/api/__generated__'
import { useSetConnectionStatus as useSetAdapterConnectionStatus } from '@/api/hooks/useProtocolAdapters/useSetConnectionStatus.tsx'
import { useSetConnectionStatus as useSetBridgeConnectionStatus } from '@/api/hooks/useGetBridges/useSetConnectionStatus.tsx'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

import ConnectionButton from './components/ConnectionButton.tsx'
import ConnectionMenu from './components/ConnectionMenu.tsx'
import { ConnectionElementProps } from '@/components/ConnectionController/types.ts'

interface ConnectionControllerProps {
  // TODO[NVL] Not sure about reusing a "proprietary" type here; simple enum like for variant?
  type: NodeTypes
  id: string
  status?: Status
  variant?: 'button' | 'menuItem'
}

const ConnectionController: FC<ConnectionControllerProps> = ({ type, id, status, variant = 'button' }) => {
  const updateAdapterStatus = useSetAdapterConnectionStatus()
  const updateBridgeStatus = useSetBridgeConnectionStatus()
  const { successToast, errorToast } = useEdgeToast()
  const { t } = useTranslation()

  const As: React.FC<ConnectionElementProps> = variant === 'button' ? ConnectionButton : ConnectionMenu

  const isRunning = status?.runtime === Status.runtime.STARTED

  const handleOnStatusChange = (eventId: string, status: StatusTransitionCommand.command) => {
    const statusPromise =
      type === NodeTypes.BRIDGE_NODE
        ? updateBridgeStatus.mutateAsync({ name: eventId, requestBody: { command: status } })
        : updateAdapterStatus.mutateAsync({ adapterId: eventId, requestBody: { command: status } })

    statusPromise
      .then(() => {
        successToast({
          title: t('protocolAdapter.toast.status.title'),
          description: t('protocolAdapter.toast.status.description'),
        })
      })
      .catch((err: ApiError) =>
        errorToast(
          {
            title: t('protocolAdapter.toast.status.title'),
            description: t('protocolAdapter.toast.status.error'),
          },
          err
        )
      )
  }

  return <As id={id} isRunning={isRunning} onChangeStatus={handleOnStatusChange} />
}

export default ConnectionController
