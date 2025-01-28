import { useMemo } from 'react'
import type { Capability } from '@/api/__generated__'

import { useGetCapabilities } from './useGetCapabilities.ts'

/**
 * Another nonsensical backend magic code that needs to be duplicated (and therefore disconnected) in the frontend
 * We have a single source of truth with OpenAPI; can we finally just use it?
 */
export enum CAPABILITY {
  PERSISTENCE = 'mqtt-persistence',
  DATAHUB = 'data-hub',
  BIDIRECTIONAL_ADAPTER = 'bi-directional protocol adapters',
  CONTROL_PLANE = 'control-plane-connectivity',
  WRITEABLE_CONFIG = 'config-writeable',
}

export const useGetCapability = (id: string) => {
  const { data, isSuccess } = useGetCapabilities()

  return useMemo<Capability | undefined>(() => {
    if (!data || !isSuccess || id.trim() === '') return undefined

    const index = data.items?.findIndex((capability) => capability.id === id)
    if (index === -1 || index === undefined) return undefined

    return data.items?.[index]
  }, [id, data, isSuccess])
}
