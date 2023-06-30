import { useState } from 'react'
import { useAuth } from '@/modules/Auth/hooks/useAuth.ts'
import { HiveMqClient } from '../../__generated__'
import config from '../../../config'

export const useHttpClient = () => {
  const { credentials } = useAuth()
  const [client] = useState<HiveMqClient>(createInstance)

  function createInstance() {
    return new HiveMqClient({
      BASE: config.apiBaseUrl,
      TOKEN: credentials?.token,
    })
  }

  return client
}
