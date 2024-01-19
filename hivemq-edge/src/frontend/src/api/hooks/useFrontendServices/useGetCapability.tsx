import { useMemo } from 'react'
import { Capability } from '@/api/__generated__'

import { useGetCapabilities } from './useGetCapabilities.tsx'
import { MOCK_CAPABILITY_DUMMY } from '@/api/hooks/useFrontendServices/__handlers__'

export enum CAPABILITY {
  PERSISTENCE = 'mqtt-persistence',
  DATAHUB = 'data-hub',
}

const getConfig = (id: string) => {
  const config: string | undefined = import.meta.env.VITE_FLAG_CAPABILITIES
  if (!config) return false

  return config.split(',').includes(id)
}

export const useGetCapability = (id: string) => {
  const { data, isSuccess } = useGetCapabilities()

  return useMemo<Capability | undefined>(() => {
    if (!data || !isSuccess || id.trim() === '') return undefined

    if (getConfig(id)) return MOCK_CAPABILITY_DUMMY

    const index = data.items?.findIndex((e) => e.id === id)
    if (index === -1 || index === undefined) return undefined

    return data.items?.[index]
  }, [id, data, isSuccess])
}
