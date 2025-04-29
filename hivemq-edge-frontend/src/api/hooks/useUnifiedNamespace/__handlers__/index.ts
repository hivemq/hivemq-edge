import type { ISA95ApiBean } from '@/api/__generated__'
import { http, HttpResponse } from 'msw'

export const mockISA95ApiBean: ISA95ApiBean = {
  enabled: false,
  prefixAllTopics: true,
  enterprise: 'enterprise',
  site: 'site',
  area: 'area',
  productionLine: 'production-line',
  workCell: 'work-cell',
}

export const handlers = [
  http.get('*/management/uns/isa95', () => {
    return HttpResponse.json<ISA95ApiBean>(mockISA95ApiBean, { status: 200 })
  }),

  http.post('*/management/uns/isa95', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
]
