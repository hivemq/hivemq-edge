import { http, HttpResponse } from 'msw'
import { DataPolicy, DataPolicyList } from '@/api/__generated__'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

export const MOCK_DATA_POLICY_ID = 'my-policy-id'

export const mockDataPolicy: DataPolicy = {
  id: MOCK_DATA_POLICY_ID,
  createdAt: MOCK_CREATED_AT,
  matching: { topicFilter: 'root/topic/ref/1' },
}

export const handlers = [
  http.get('*/data-hub/data-validation/policies', () => {
    return HttpResponse.json<DataPolicyList>(
      {
        items: [mockDataPolicy],
      },
      { status: 200 }
    )
  }),
]
