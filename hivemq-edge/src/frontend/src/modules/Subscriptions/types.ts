import { type RJSFSchema, type UiSchema } from '@rjsf/utils'

export interface SubscriptionManagerType {
  schema: RJSFSchema
  // TODO[NVL] Needs to align the types for the subscriptions
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  formData: { subscriptions: Record<string, any> }
  uiSchema: UiSchema
  onSubmit?: (data: unknown) => void
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
