import { http, HttpResponse } from 'msw'
import type { FsmStateInformationItem, FsmStatesInformationListItem } from '@/api/__generated__'

export const mockFSMStateInfo: FsmStateInformationItem = {}

export const handlers = [
  http.get('*/data-hub/behavior-validation/states/:clientId', () => {
    return HttpResponse.json<FsmStatesInformationListItem>({ items: [mockFSMStateInfo] }, { status: 200 })
  }),
]
