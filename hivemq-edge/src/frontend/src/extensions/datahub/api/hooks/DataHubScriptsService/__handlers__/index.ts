import { rest } from 'msw'

import { Script, type ScriptList } from '@/api/__generated__'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'

export const MOCK_SCRIPT_ID = 'my-script-id'
export const MOCK_SCRIPT_SOURCE = `
function convert(fahrenheit) {
    return Mah.floor((fahrenheit - 32) * 5/9);
}

function transform(publish, context) {
     publish.payload = {
        "celsius": convert(publish.payload.fahrenheit),
        "timestamp": publish.payload.timestamp
    }
    return publish;
}
`

export const mockScript: Script = {
  id: MOCK_SCRIPT_ID,
  createdAt: MOCK_CREATED_AT,
  functionType: Script.functionType.TRANSFORMATION,
  source: btoa(JSON.stringify(MOCK_SCRIPT_SOURCE)),
}

export const handlers = [
  rest.get('*/data-hub/scripts', (_, res, ctx) => {
    return res(
      ctx.json<ScriptList>({
        items: [mockScript],
      }),
      ctx.status(200)
    )
  }),
]
