import { ISA95ApiBean } from '@/api/__generated__'
import { rest } from 'msw'

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
  rest.get('*/management/uns/isa95', (_, res, ctx) => {
    return res(ctx.json<ISA95ApiBean>(mockISA95ApiBean), ctx.status(200))
  }),

  rest.post('*/management/uns/isa95', (_, res, ctx) => {
    // @ts-ignore
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { adapterType } = req.params
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res(ctx.json<any>({}), ctx.status(200))
  }),
]
