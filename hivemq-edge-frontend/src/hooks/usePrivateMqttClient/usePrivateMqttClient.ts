import { useContext } from 'react'
import { PrivateMqttClientContext } from '@/hooks/usePrivateMqttClient/PrivateMqttClientContext.ts'

export const usePrivateMqttClient = () => {
  return useContext(PrivateMqttClientContext)
}
