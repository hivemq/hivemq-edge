import { BehaviorPolicyTransitionEvent } from '@/api/__generated__'
import { expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

import { server } from '@/__test-utils__/msw/mockServer.ts'
import { SimpleWrapper as wrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

import { handlers } from './__handlers__'
import { useGetAllFunctionSpecs } from './useGetAllFunctionSpecs.ts'

describe('useGetAllFunctionSpecs', () => {
  afterEach(() => {
    server.resetHandlers()
  })

  it('should load the data', async () => {
    server.use(...handlers)

    const { result } = renderHook(useGetAllFunctionSpecs, { wrapper })
    await waitFor(() => {
      expect(result.current.isLoading).toBeFalsy()
      expect(result.current.isSuccess).toBeTruthy()
    })
    expect(result.current.data).toStrictEqual({
      items: expect.arrayContaining([
        expect.objectContaining({
          functionId: 'Mqtt.UserProperties.add',
          metadata: {
            hasArguments: true,
            inLicenseAllowed: false,
            isDataOnly: false,
            isTerminal: false,
            supportedEvents: [
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
            ],
          },
          schema: expect.objectContaining({
            title: 'Mqtt.UserProperties.add',
          }),
        }),
        expect.objectContaining({
          functionId: 'Delivery.redirectTo',
          metadata: {
            inLicenseAllowed: true,
            isTerminal: true,
            isDataOnly: true,
            hasArguments: true,
            supportedEvents: [],
          },
          schema: expect.objectContaining({
            title: 'Delivery.redirectTo',
          }),
        }),
        expect.objectContaining({
          functionId: 'System.log',
          metadata: {
            inLicenseAllowed: false,
            isTerminal: false,
            isDataOnly: false,
            hasArguments: true,
            supportedEvents: [
              BehaviorPolicyTransitionEvent.EVENT_ON_ANY,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
              BehaviorPolicyTransitionEvent.CONNECTION_ON_DISCONNECT,
            ],
          },
          schema: expect.objectContaining({
            title: 'System.log',
          }),
        }),
        expect.objectContaining({
          functionId: 'Serdes.serialize',
          metadata: {
            inLicenseAllowed: true,
            isTerminal: false,
            isDataOnly: true,
            hasArguments: true,
            supportedEvents: [],
          },
          schema: expect.objectContaining({
            title: 'Serdes.serialize',
          }),
        }),
        expect.objectContaining({
          functionId: 'Serdes.deserialize',
          metadata: {
            hasArguments: true,
            inLicenseAllowed: true,
            isDataOnly: true,
            isTerminal: false,
            supportedEvents: [],
          },
          schema: expect.objectContaining({
            title: 'Serdes.deserialize',
          }),
        }),
        expect.objectContaining({
          functionId: 'Metrics.Counter.increment',
          metadata: {
            hasArguments: true,
            inLicenseAllowed: false,
            isDataOnly: false,
            isTerminal: false,
            supportedEvents: [
              BehaviorPolicyTransitionEvent.EVENT_ON_ANY,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_CONNECT,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_PUBLISH,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_SUBSCRIBE,
              BehaviorPolicyTransitionEvent.MQTT_ON_INBOUND_DISCONNECT,
              BehaviorPolicyTransitionEvent.CONNECTION_ON_DISCONNECT,
            ],
          },
          schema: expect.objectContaining({
            title: 'Metrics.Counter.increment',
          }),
        }),
        expect.objectContaining({
          functionId: 'Mqtt.disconnect',
          metadata: {
            hasArguments: false,
            inLicenseAllowed: false,
            isDataOnly: false,
            isTerminal: true,
            supportedEvents: ['Mqtt.OnInboundConnect', 'Mqtt.OnInboundPublish', 'Mqtt.OnInboundSubscribe'],
          },
          schema: expect.objectContaining({
            title: 'Mqtt.disconnect',
          }),
        }),
        expect.objectContaining({
          functionId: 'Mqtt.drop',
          metadata: {
            hasArguments: true,
            inLicenseAllowed: false,
            isDataOnly: false,
            isTerminal: true,
            supportedEvents: ['Mqtt.OnInboundPublish', 'Mqtt.OnInboundSubscribe'],
          },
          schema: expect.objectContaining({
            title: 'Mqtt.drop',
          }),
        }),
      ]),
    })
  })
})
