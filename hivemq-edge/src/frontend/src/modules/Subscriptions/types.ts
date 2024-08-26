import { type RJSFSchema, type UiSchema } from '@rjsf/utils'

export interface SubscriptionManagerType {
  schema: RJSFSchema
  formData: { subscriptions: Record<string, unknown> }
  uiSchema: UiSchema
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export type OutwardSubscription = {}
