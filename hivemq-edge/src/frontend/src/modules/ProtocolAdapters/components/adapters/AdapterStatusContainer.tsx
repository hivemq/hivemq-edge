import type { FC } from 'react'
import { useGetAdaptersStatus } from '@/api/hooks/useConnection/useGetAdaptersStatus.ts'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'

export const AdapterStatusContainer: FC<{ id: string }> = ({ id }) => {
  const { data: connections } = useGetAdaptersStatus()

  const connection = connections?.items?.find((e) => e.id === id)

  return <ConnectionStatusBadge status={connection} />
}
