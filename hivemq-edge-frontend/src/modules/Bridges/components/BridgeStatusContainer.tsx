import type { FC } from 'react'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import { useGetBridgesStatus } from '@/api/hooks/useConnection/useGetBridgesStatus.ts'

export const BridgeStatusContainer: FC<{ id: string }> = ({ id }) => {
  const { data: connections } = useGetBridgesStatus()

  const connection = connections?.items?.find((status) => status.id === id)

  return <ConnectionStatusBadge status={connection} />
}
