import { FC, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { ApiError, Status, StatusTransitionCommand } from '@/api/__generated__'
import { DeviceTypes } from '@/api/types/api-devices.ts'
import { useSetConnectionStatus as useSetAdapterConnectionStatus } from '@/api/hooks/useProtocolAdapters/useSetConnectionStatus.tsx'
import { useSetConnectionStatus as useSetBridgeConnectionStatus } from '@/api/hooks/useGetBridges/useSetConnectionStatus.tsx'

import { useEdgeToast } from '@/hooks/useEdgeToast/useEdgeToast.tsx'

import ConnectionButton from './components/ConnectionButton.tsx'
import ConnectionMenu from './components/ConnectionMenu.tsx'
import { ConnectionElementProps } from '@/components/ConnectionController/types.ts'

interface ConnectionControllerProps {
  type: DeviceTypes
  id: string
  status?: Status
  variant?: 'button' | 'menuItem'
}

const ConnectionController: FC<ConnectionControllerProps> = ({ type, id, status, variant = 'button' }) => {
  const updateAdapterStatus = useSetAdapterConnectionStatus()
  const updateBridgeStatus = useSetBridgeConnectionStatus()
  const { successToast, errorToast } = useEdgeToast()
  const { t } = useTranslation()
  const [isLoading, setIsLoading] = useState(0)

  const As: React.FC<ConnectionElementProps> = variant === 'button' ? ConnectionButton : ConnectionMenu

  const isRunning = status?.runtime === Status.runtime.STARTED

  useEffect(() => {
    if (isLoading) {
      const timer = setTimeout(() => {
        setIsLoading(0)
      }, isLoading)

      return () => {
        clearTimeout(timer)
      }
    }
  }, [isLoading])

  const handleOnStatusChange = (eventId: string, status: StatusTransitionCommand.command) => {
    const statusPromise =
      type === DeviceTypes.BRIDGE
        ? updateBridgeStatus.mutateAsync({ name: eventId, requestBody: { command: status } })
        : updateAdapterStatus.mutateAsync({ adapterId: eventId, requestBody: { command: status } })

    statusPromise
      .then((results) => {
        const { callbackTimeoutMillis } = results
        if (callbackTimeoutMillis) setIsLoading(callbackTimeoutMillis)
        successToast({
          title: t('protocolAdapter.toast.status.title'),
          description: t('protocolAdapter.toast.status.description', {
            context: status,
            device: type,
          }),
        })
      })
      .catch((err: ApiError) =>
        errorToast(
          {
            title: t('protocolAdapter.toast.status.title'),
            description: t('protocolAdapter.toast.status.error', {
              device: type,
            }),
          },
          err
        )
      )
  }

  return <As id={id} isRunning={isRunning} onChangeStatus={handleOnStatusChange} isLoading={!!isLoading} />
}

export default ConnectionController
