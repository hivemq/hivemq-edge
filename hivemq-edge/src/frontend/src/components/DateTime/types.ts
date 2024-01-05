import { GetOptionLabel, GetOptionValue } from 'chakra-react-select'
import { ThemeTypings } from '@chakra-ui/react'
import { Duration } from 'luxon'

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
