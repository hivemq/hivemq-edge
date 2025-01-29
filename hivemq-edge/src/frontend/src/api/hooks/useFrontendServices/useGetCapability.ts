import { useMemo } from 'react'
import type { Capability } from '@/api/__generated__'

import { useGetCapabilities } from './useGetCapabilities.ts'

export const useGetCapability = (id: Capability.id) => {
  const { data, isSuccess } = useGetCapabilities()

  return useMemo<Capability | undefined>(() => {
    if (!data || !isSuccess || id.trim() === '') return undefined

    const index = data.items?.findIndex((capability) => capability.id === id)
    if (index === -1 || index === undefined) return undefined

    return data.items?.[index]
  }, [id, data, isSuccess])
}
