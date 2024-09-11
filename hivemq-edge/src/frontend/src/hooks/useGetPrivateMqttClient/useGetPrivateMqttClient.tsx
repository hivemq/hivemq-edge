import { useCallback, useEffect, useState } from 'react'
import mqtt, { MqttClient, ErrorWithReasonCode } from 'mqtt'

import { MqttClientStatus } from '@/hooks/useGetPrivateMqttClient/type.ts'
import { PRIVATE_MQTT_CLIENT } from '@/hooks/useGetPrivateMqttClient/mqtt-client.utils.ts'

export const useGetPrivateMqttClient = () => {
  const [client, setClient] = useState<MqttClient | null>(null)
  const [connectStatus, setConnectStatus] = useState<MqttClientStatus>(MqttClientStatus.DISCONNECTED)
  const [error, setError] = useState<Error | ErrorWithReasonCode | null>(null)

  const mqttConnect = () => {
    setConnectStatus(MqttClientStatus.CONNECTING)
    mqtt
      .connectAsync(PRIVATE_MQTT_CLIENT)
      .then((client) => {
        setClient(client)
        setConnectStatus(MqttClientStatus.CONNECTED)
        setError(null)
      })
      .catch((error: Error | ErrorWithReasonCode) => {
        setConnectStatus(MqttClientStatus.CLIENT_ERROR)
        setError(error)
      })
  }

  const mqttDisconnect = useCallback(() => {
    try {
      client?.end(false, () => {
        setConnectStatus(MqttClientStatus.DISCONNECTED)
        setError(null)
      })
    } catch (error) {
      setConnectStatus(MqttClientStatus.CLIENT_ERROR)
      setError(error as Error | ErrorWithReasonCode)
    }
  }, [client])

  const mqttSub = useCallback(
    (topic: string) => {
      if (!client) return
      client.subscribe(topic, undefined, (error) => {
        if (error) {
          setConnectStatus(MqttClientStatus.CLIENT_ERROR)
          setError(error as Error | ErrorWithReasonCode)
          return
        }
      })
    },
    [client]
  )

  const mqttUnSub = useCallback(
    (topic: string) => {
      if (client) {
        client.unsubscribe(topic, undefined, (error) => {
          if (error) {
            setConnectStatus(MqttClientStatus.CLIENT_ERROR)
            setError(error as Error | ErrorWithReasonCode)
            return
          }
        })
      }
    },
    [client]
  )

  ///// Lifecycle management of the private MQTT client

  useEffect(() => {
    mqttConnect()
  }, [])

  useEffect(() => {
    return () => {
      if (client) mqttDisconnect()
    }
  }, [client, mqttDisconnect])

  useEffect(() => {
    if (!client) return

    client.on('connect', () => {
      setConnectStatus(MqttClientStatus.CONNECTED)
    })

    client.on('error', (error) => {
      setConnectStatus(MqttClientStatus.CLIENT_ERROR)
      setError(error as Error | ErrorWithReasonCode)
      // Should we disconnect as in the original code?
      mqttDisconnect()
    })

    client.on('reconnect', () => {
      setConnectStatus(MqttClientStatus.RECONNECTING)
    })
  }, [client, connectStatus, mqttDisconnect])

  return {
    isLoading: connectStatus === MqttClientStatus.SAMPLING,
    isError: connectStatus === MqttClientStatus.CLIENT_ERROR,
    error,
  }
}
