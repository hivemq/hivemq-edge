import { describe, expect } from 'vitest'

import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import { getOperationSchema } from './OperationPanel.utils.ts'

describe('getOperationSchema', () => {
  it('should return the uiSchema', async () => {
    const { uiSchema } = getOperationSchema(MOCK_DATAHUB_FUNCTIONS.items)
    expect(uiSchema).toStrictEqual(
      expect.objectContaining({
        formData: expect.objectContaining({
          message: expect.objectContaining({
            'ui:widget': 'datahub:message-interpolation',
          }),
          metricName: expect.objectContaining({
            'ui:widget': 'datahub:metric-counter',
          }),
        }),
        functionId: expect.objectContaining({
          'ui:widget': 'datahub:function-selector',
        }),
      })
    )
  })

  it('should return the schema', async () => {
    const { schema } = getOperationSchema(MOCK_DATAHUB_FUNCTIONS.items)
    expect(schema).toStrictEqual(
      expect.objectContaining({
        $ref: '#/definitions/functionId',

        definitions: expect.objectContaining({
          'Delivery.redirectTo': expect.objectContaining({}),
          'Metrics.Counter.increment': expect.objectContaining({}),
          'Mqtt.UserProperties.add': expect.objectContaining({}),
          'Mqtt.disconnect': expect.objectContaining({}),
          'Mqtt.drop': expect.objectContaining({}),
          'Serdes.deserialize': expect.objectContaining({}),
          'Serdes.serialize': expect.objectContaining({}),
          'System.log': expect.objectContaining({}),
          functionId: expect.objectContaining({
            properties: expect.objectContaining({}),
            dependencies: expect.objectContaining({}),
          }),
        }),
      })
    )
  })
})
