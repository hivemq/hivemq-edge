import { type RJSFSchema, type UiSchema } from '@rjsf/utils'

export interface SubscriptionManagerType {
  schema: RJSFSchema
  formData: { subscriptions: Record<string, unknown> }
  uiSchema: UiSchema
  onSubmit?: (data: any) => void
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface OutwardSubscription {
  node: string
  mqttTopic: string[]
  mapping: Mapping[]
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface Mapping {
  source: string[]
  destination: string
  transformation: Transformation
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export interface Transformation {
  function: 'toString' | 'toInt' | 'join'
  params: string
}
