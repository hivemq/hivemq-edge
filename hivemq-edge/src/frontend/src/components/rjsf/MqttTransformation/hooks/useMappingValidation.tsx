import { MappingValidation, OutwardMapping } from '@/modules/Mappings/types.ts'

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const useMappingValidation = (_item: OutwardMapping) => {
  const status: MappingValidation = { status: 'error', errors: [] }
  return status
}
