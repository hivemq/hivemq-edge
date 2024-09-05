import { MappingValidation, OutwardSubscription } from '@/modules/Subscriptions/types.ts'

export const useMappingValidation = (item: OutwardSubscription) => {
  const status: MappingValidation = { status: 'error', errors: [] }
  return status
}
