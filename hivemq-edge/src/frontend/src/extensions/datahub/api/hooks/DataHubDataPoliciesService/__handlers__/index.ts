import { rest } from 'msw'
import { DataPolicy, DataPolicyList } from '@/api/__generated__'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

export const MOCK_DATA_POLICY_ID = 'my-policy-id'

export const mockDataPolicy: DataPolicy = {
  id: MOCK_DATA_POLICY_ID,
  createdAt: MOCK_CREATED_AT,
  matching: { topicFilter: 'topic/mock/1' },
}

export const handlers = [
  rest.get('*/data-hub/data-validation/policies', (_, res, ctx) => {
    return res(
      ctx.json<DataPolicyList>({
        items: [mockDataPolicy],
      }),
      ctx.status(200)
    )
  }),
]
