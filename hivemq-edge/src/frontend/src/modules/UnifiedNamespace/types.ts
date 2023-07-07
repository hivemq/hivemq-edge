import { ISA95ApiBean } from '@/api/__generated__'

export type ISA95Namespace = Pick<ISA95ApiBean, 'enterprise' | 'site' | 'area' | 'productionLine' | 'workCell'>
