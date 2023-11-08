import { GetOptionLabel, GetOptionValue } from 'chakra-react-select'

// Not properly exported from chakra-react-select
export interface Accessors<Option> {
  getOptionValue: GetOptionValue<Option>
  getOptionLabel: GetOptionLabel<Option>
}

export interface RangeOption {
  readonly value: string
  readonly label: string
  readonly color: string
  readonly isDisabled?: boolean
}
