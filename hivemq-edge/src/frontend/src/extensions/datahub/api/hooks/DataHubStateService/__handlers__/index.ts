import { http, HttpResponse } from 'msw'
import { FsmStateInformationItem, FsmStatesInformationListItem } from '@/api/__generated__'

export const mockFSMStateInfo: FsmStateInformationItem = {}

export const handlers = [
  http.get('*/data-hub/fsm', () => {
    return HttpResponse.json<FsmStatesInformationListItem>({ items: [mockFSMStateInfo] }, { status: 200 })
  }),
]
