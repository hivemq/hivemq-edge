import { FC, PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react'
import mqtt, { ErrorWithReasonCode, MqttClient } from 'mqtt'
import debug from 'debug'

import { MqttClientStatus, MQTTSample, PrivateMqttClientType } from '@/hooks/usePrivateMqttClient/type.ts'
import { PRIVATE_MQTT_CLIENT, SAMPLING_DURATION } from '@/hooks/usePrivateMqttClient/mqtt-client.utils.ts'
import { PrivateMqttClientContext } from '@/hooks/usePrivateMqttClient/PrivateMqttClientContext'

const mqttClientLog = debug('MqttClient')

const useUpdatingRef = <T,>(value: T) => {
  const ref = useRef(value)
  ref.current = value
  return ref
}

export const PrivateMqttClientProvider: FC<PropsWithChildren> = ({ children }) => {
  const [client, setClient] = useState<MqttClient | null>(null)
  const [connectStatus, setConnectStatus] = useState<MqttClientStatus>(MqttClientStatus.DISCONNECTED)
  const [samples, setSamples] = useState<MQTTSample[]>([])
  const [error, setError] = useState<Error | ErrorWithReasonCode | null>(null)
  const samplesLockedRef = useUpdatingRef<MQTTSample[]>(samples)

  ///// MQTT client Operations

  // https://github.com/mqttjs/MQTT.js#connect
  const mqttConnect = useCallback(() => {
    if (client) return
    setConnectStatus(MqttClientStatus.CONNECTING)
    setClient(mqtt.connect(PRIVATE_MQTT_CLIENT))
    mqttClientLog('connect')
  }, [client])

  // https://github.com/mqttjs/MQTT.js#mqttclientendforce-options-callback
  const mqttDisconnect = useCallback(() => {
    if (!client) return
    try {
      client.end(false, () => {
        mqttClientLog('disconnect ')
        setConnectStatus(MqttClientStatus.DISCONNECTED)
        setClient(null)
        setError(null)
      })
    } catch (error) {
      setConnectStatus(MqttClientStatus.CLIENT_ERROR)
      setError(error as Error | ErrorWithReasonCode)
      mqttClientLog('disconnect error:', error)
    }
  }, [client])

  // https://github.com/mqttjs/MQTT.js#mqttclientsubscribetopictopic-arraytopic-object-options-callback
  const mqttSubscribe = useCallback(
    (topic: string) => {
      if (client) {
        client.subscribe(topic, undefined, (error) => {
          if (error) {
            setConnectStatus(MqttClientStatus.CLIENT_ERROR)
            setError(error as Error | ErrorWithReasonCode)
            mqttClientLog('Subscribe to topics error', error)
            return
          }
          mqttClientLog(`Subscribe to topics: ${topic}`)
        })
      }
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
            mqttClientLog('Unsubscribe error', error)
            return
          }
          mqttClientLog(`Unsubscribed topic: ${topic}`)
        })
      }
    },
    [client]
  )

  ///// Lifecycle management of the MQTT client
  useEffect(() => {
    mqttConnect()
  }, [mqttConnect])

  useEffect(() => {
    return () => mqttDisconnect()
  }, [mqttDisconnect])

  useEffect(() => {
    if (!client) return

    // https://github.com/mqttjs/MQTT.js#event-connect
    client.on('connect', () => {
      mqttClientLog('connect')
      setConnectStatus(MqttClientStatus.CONNECTED)
    })

    // https://github.com/mqttjs/MQTT.js#event-error
    client.on('error', (error) => {
      setConnectStatus(MqttClientStatus.CLIENT_ERROR)
      setError(error as Error | ErrorWithReasonCode)
      mqttClientLog('Connection error: ', error)
      mqttDisconnect()
    })

    // https://github.com/mqttjs/MQTT.js#event-reconnect
    client.on('reconnect', () => {
      mqttClientLog('reconnect')
      setConnectStatus(MqttClientStatus.RECONNECTING)
    })

    // https://github.com/mqttjs/MQTT.js#event-message
    client.on('message', (topic, message) => {
      if (connectStatus === MqttClientStatus.SAMPLING) {
        // setSamples((old) => Array.from(new Set([...old, topic])))
        // const schema = GenerateSchema.json('topic', JSON.parse(message.toString()))
        setSamples((old) => {
          const topics = old.map((sample) => sample.topic)
          if (topics.includes(topic)) return [...old]

          mqttClientLog('MQTTClient: message', topic)
          return [...old, { payload: JSON.parse(message.toString()), topic: topic }]
        })
      }
    })
  }, [client, connectStatus, mqttDisconnect])

  ///// Sampling async method

  // TODO[NVL] Instead of using wildcard, add multiple subscriptions
  const onSampling = useCallback(
    (topicFilter: string) => {
      if (!client) return
      if (connectStatus === MqttClientStatus.SAMPLING) return

      mqttClientLog('start')
      setError(null)
      setSamples([])
      setConnectStatus(MqttClientStatus.SAMPLING)
      mqttSubscribe(topicFilter)

      return new Promise<MQTTSample[]>((resolve) => {
        setTimeout(() => {
          mqttUnsubscribe(topicFilter)
          setConnectStatus(MqttClientStatus.CONNECTED)
          resolve(samplesLockedRef.current)
        }, SAMPLING_DURATION)
      })
    },
    [client, connectStatus, mqttSubscribe, mqttUnsubscribe, samplesLockedRef]
  )

  const value: PrivateMqttClientType = {
    state: { client, connectStatus, samples, error },
    actions: { onSampling },
  }

  return <PrivateMqttClientContext.Provider value={value}>{children}</PrivateMqttClientContext.Provider>
}
