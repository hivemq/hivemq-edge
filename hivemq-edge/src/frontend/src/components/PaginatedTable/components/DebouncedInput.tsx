import React from 'react'
import { IconButton, Input, InputGroup, InputRightElement } from '@chakra-ui/react'
import { CloseIcon } from '@chakra-ui/icons'
import { useTranslation } from 'react-i18next'

export const DebouncedInput = ({
  value: initialValue,
  onChange,
  id,
  debounce = 500,
  ...props
}: {
  value: string | number
  id: string
  onChange: (value: string | number) => void
  debounce?: number
} & Omit<React.InputHTMLAttributes<HTMLInputElement>, 'onChange'>) => {
  const { t } = useTranslation()
  const [value, setValue] = React.useState(initialValue)

  React.useEffect(() => {
    setValue(initialValue)
  }, [initialValue])

  React.useEffect(() => {
    const timeout = setTimeout(() => {
      onChange(value)
    }, debounce)

    return () => clearTimeout(timeout)
  }, [debounce, onChange, value])

  return (
    <InputGroup size="md">
      <Input {...props} size="sm" value={value} onChange={(e) => setValue(e.target.value)} />
      <InputRightElement h={'32px'}>
        <IconButton
          data-testid={id + '-clear'}
          isDisabled={value === ''}
          size="xs"
          variant={'ghost'}
          onClick={() => onChange('')}
          aria-label={t('components:pagination.filter.clear')}
          icon={<CloseIcon />}
        />
      </InputRightElement>
    </InputGroup>
  )
}
