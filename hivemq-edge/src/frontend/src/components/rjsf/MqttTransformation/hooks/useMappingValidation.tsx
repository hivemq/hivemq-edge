import { MappingValidation, OutwardSubscription } from '@/modules/Subscriptions/types.ts'

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const useMappingValidation = (_item: OutwardSubscription) => {
  const status: MappingValidation = { status: 'success', errors: [] }
  return status
}
