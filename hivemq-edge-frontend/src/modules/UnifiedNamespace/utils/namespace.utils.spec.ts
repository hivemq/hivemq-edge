import { expect } from 'vitest'

import type { ISA95Namespace } from '../types.ts'
import { namespaceToStrings } from '@/modules/UnifiedNamespace/utils/namespace.utils.ts'

type MatchSuite = [ISA95Namespace, string[]]

describe('namespaceToStrings', () => {
  it.each<MatchSuite>([
    [{}, []],
    [{ enterprise: 'enterprise' }, ['enterprise']],
    [{ area: 'area' }, ['area']],
    [{ area: 'area', enterprise: 'enterprise' }, ['enterprise', 'area']],
    [{ enterprise: 'enterprise', area: 'area' }, ['enterprise', 'area']],
    [
      { area: 'area', enterprise: 'enterprise', productionLine: 'productionLine', site: 'site', workCell: 'workCell' },
      ['enterprise', 'site', 'area', 'productionLine', 'workCell'],
    ],
  ])('should export %s as %s ', (ns, output) => {
    expect(namespaceToStrings(ns)).toStrictEqual(output)
  })
})
