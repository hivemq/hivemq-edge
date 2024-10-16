import { createContext } from 'react'
import { PrivateMqttClientType } from '@/hooks/usePrivateMqttClient/type.ts'

export const PrivateMqttClientContext = createContext<PrivateMqttClientType>({ state: undefined, actions: undefined })
