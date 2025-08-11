import { useMemo } from 'react'
import type { UseBaseQueryResult } from '@tanstack/react-query'
import type { ApiError, Capability } from '@/api/__generated__'

import { useGetCapabilities } from './useGetCapabilities.ts'

export const useGetCapability = (id: Capability.id) => {
  const { data, isSuccess, ...rest } = useGetCapabilities()

  const capability = useMemo<Capability | undefined>(() => {
    if (!data || !isSuccess || id.trim() === '') return undefined

    const index = data.items?.findIndex((capability) => capability.id === id)
    if (index === -1 || index === undefined) return undefined

    return data.items?.[index]
  }, [id, data, isSuccess])

  return { data: capability, isSuccess, ...rest } as UseBaseQueryResult<Capability, ApiError>
}
