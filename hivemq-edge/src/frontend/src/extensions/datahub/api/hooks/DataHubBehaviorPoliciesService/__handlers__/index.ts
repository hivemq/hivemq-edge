import { http, HttpResponse } from 'msw'
import { type BehaviorPolicy, type BehaviorPolicyList } from '@/api/__generated__'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

export const MOCK_BEHAVIOR_POLICY_ID = 'my-behavior-policy-id'

export const mockBehaviorPolicy: BehaviorPolicy = {
  id: MOCK_BEHAVIOR_POLICY_ID,
  createdAt: MOCK_CREATED_AT,
  matching: { clientIdRegex: 'client-mock-1' },
  behavior: { id: 'fgf' },
}

export const handlers = [
  http.get('*/data-hub/behavior-validation/policies', () => {
    return HttpResponse.json<BehaviorPolicyList>(
      {
        items: [mockBehaviorPolicy],
      },
      { status: 200 }
    )
  }),
]
