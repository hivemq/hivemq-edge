import { useMemo } from 'react'
import { Capability } from '@/api/__generated__'

import { useGetCapabilities } from './useGetCapabilities.tsx'

export const useGetCapability = (id: string) => {
  const { data, isSuccess } = useGetCapabilities()

  return useMemo<Capability | undefined>(() => {
    if (!data || !isSuccess || id.trim() === '') return undefined

    const index = data.items?.findIndex((e) => e.id === id)
    if (index === -1 || index === undefined) return undefined

    return data.items?.[index]
  }, [id, data, isSuccess])
}
