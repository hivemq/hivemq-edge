import type { GetOptionLabel, GetOptionValue } from 'chakra-react-select'
import type { ThemeTypings } from '@chakra-ui/react'
import type { Duration } from 'luxon'

// Not properly exported from chakra-react-select
export interface Accessors<Option> {
  getOptionValue: GetOptionValue<Option>
  getOptionLabel: GetOptionLabel<Option>
}

export interface RangeOption {
  readonly value: string
  readonly label: string
  readonly colorScheme: ThemeTypings['colorSchemes']
  readonly isCommand?: boolean
  readonly isDisabled?: boolean
  readonly duration?: Duration
}
