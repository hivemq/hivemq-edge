import { useCallback, useEffect, useState } from 'react'
import mqtt, { MqttClient, ErrorWithReasonCode } from 'mqtt'

import { MqttClientStatus, MQTTSample } from '@/hooks/useGetPrivateMqttClient/type.ts'
import { PRIVATE_MQTT_CLIENT, SAMPLING_DURATION } from '@/hooks/useGetPrivateMqttClient/mqtt-client.utils.ts'

export const useGetPrivateMqttClient = () => {
  const [client, setClient] = useState<MqttClient | null>(null)
  const [connectStatus, setConnectStatus] = useState<MqttClientStatus>(MqttClientStatus.DISCONNECTED)
  const [samples, setSamples] = useState<MQTTSample[]>([])
  const [error, setError] = useState<Error | ErrorWithReasonCode | null>(null)

  // https://github.com/mqttjs/MQTT.js#connect
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

  // https://github.com/mqttjs/MQTT.js#mqttclientendforce-options-callback
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

  // https://github.com/mqttjs/MQTT.js#mqttclientsubscribetopictopic-arraytopic-object-options-callback
  const mqttSubscribe = useCallback(
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

  // https://github.com/mqttjs/MQTT.js#mqttclientunsubscribetopictopic-array-options-callback
  const mqttUnsubscribe = useCallback(
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

    // https://github.com/mqttjs/MQTT.js#event-connect
    client.on('connect', () => {
      setConnectStatus(MqttClientStatus.CONNECTED)
    })

    // https://github.com/mqttjs/MQTT.js#event-error
    client.on('error', (error) => {
      setConnectStatus(MqttClientStatus.CLIENT_ERROR)
      setError(error as Error | ErrorWithReasonCode)
      // Should we disconnect as in the original code?
      mqttDisconnect()
    })

    // https://github.com/mqttjs/MQTT.js#event-reconnect
    client.on('reconnect', () => {
      setConnectStatus(MqttClientStatus.RECONNECTING)
    })

    // https://github.com/mqttjs/MQTT.js#event-message
    client.on('message', (topic, message) => {
      if (connectStatus === MqttClientStatus.SAMPLING) {
        setSamples((old) => {
          const gg = old.map((e) => e.topic)
          if (gg.includes(topic)) return [...old]
          console.info('message', topic)
          return [...old, { payload: JSON.parse(message.toString()), topic: topic }]
        })
      }
    })
  }, [client, connectStatus, mqttDisconnect])

  ///// Sampling async method
  const mqttSampling = (topic = '#'): Promise<MQTTSample[]> => {
    setConnectStatus(MqttClientStatus.SAMPLING)
    setSamples([])
    mqttSubscribe(topic)
    return new Promise<MQTTSample[]>((resolve) => {
      setTimeout(() => {
        mqttUnsubscribe(topic)
        setConnectStatus(MqttClientStatus.CONNECTED)
        resolve(samples)
      }, SAMPLING_DURATION)
    })
  }

  return {
    isLoading: connectStatus === MqttClientStatus.SAMPLING,
    isError: connectStatus === MqttClientStatus.CLIENT_ERROR,
    mqttSampling,
    samples,
    error,
  }
}
