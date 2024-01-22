import { ISA95ApiBean } from '@/api/__generated__'
import { DateTime } from 'luxon'

export const MOCK_CREATED_AT = DateTime.fromISO('2023-10-13T11:51:24.234').toString()

export const MOCK_JWT =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
  'eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.' +
  'SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c'

export const MOCK_BREADCRUMB = ['Enterprise', 'Site', 'Area', 'Line', 'Cell']
export const MOCK_NAMESPACE: ISA95ApiBean = {
  enterprise: MOCK_BREADCRUMB[0],
  site: MOCK_BREADCRUMB[1],
  area: MOCK_BREADCRUMB[2],
  productionLine: MOCK_BREADCRUMB[3],
  workCell: MOCK_BREADCRUMB[4],
}

export const MOCK_BRIDGE_ID = 'first-bridge'
export const MOCK_ADAPTER_ID = 'my-adapter'
