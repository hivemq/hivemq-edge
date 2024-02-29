import { GroupBase, OptionProps } from 'chakra-react-select'

import { RangeOption } from '../types.ts'
import Option from '../components/Option.tsx'
import { MOCK_RANGE_OPTION } from '@/components/DateTime/utils/range-option.mocks.ts'

const MOCK_OPTIONS: readonly RangeOption[] = [{ value: 'purple', label: 'last minute', colorScheme: '#5243AA' }]

const MOCK_PROPS: Partial<OptionProps<RangeOption, false, GroupBase<RangeOption>>> = {
  hasValue: false,
  isMulti: false,
  isRtl: false,
  options: [MOCK_RANGE_OPTION],
  // @ts-ignore
  selectProps: {
    chakraStyles: {},
  },
  innerProps: {
    id: 'react-select-15-option-3',
    tabIndex: -1,
  },
  data: MOCK_RANGE_OPTION,
  isDisabled: false,
  isSelected: false,
  label: 'last 30 minutes',
  type: 'option',
  value: 'minute30',
  isFocused: false,
  clearValue: () => console.log('sss'),
  cx: () => '',
  selectOption: (x) => console.log('select', x),
  setValue: (x) => console.log('select', x),
}

describe('DateTimeRangeSelector > Option', () => {
  beforeEach(() => {
    cy.viewport(800, 150)
  })

  it('should render properly', () => {
    // @ts-ignore force mocked partial object
    cy.mountWithProviders(<Option children={MOCK_OPTIONS[0].label} {...MOCK_PROPS} />)

    // cy.getByAriaLabel('Go to the first page').should('be.visible').click()
    // cy.get('@setPageIndex').should('have.been.calledOnceWith', 0)
  })
})
