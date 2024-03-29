import { rest } from 'msw'
import { type FsmStateInformationItem, type FsmStatesInformationListItem } from '@/api/__generated__'

export const mockFSMStateInfo: FsmStateInformationItem = {}

export const handlers = [
  rest.get('*/data-hub/behavior-validation/states/:clientId', (_, res, ctx) => {
    return res(
      ctx.json<FsmStatesInformationListItem>({
        items: [mockFSMStateInfo],
      }),
      ctx.status(200)
    )
  }),
]
