import { http, HttpResponse } from 'msw'

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
  version: 1,
  description: 'this is a description',
}

export const handlers = [
  http.get('*/data-hub/scripts', () => {
    return HttpResponse.json<ScriptList>(
      {
        items: [mockScript],
      },
      { status: 200 }
    )
  }),

  http.get('*/data-hub/scripts/:scriptid', () => {
    return HttpResponse.json<Script>(mockScript, { status: 200 })
  }),

  http.post('*/data-hub/scripts', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.delete('*/data-hub/scripts/:scriptid', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  http.put('*/data-hub/scripts/:scriptid', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
]
