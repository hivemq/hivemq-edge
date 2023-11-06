import { FC, KeyboardEventHandler, MutableRefObject, useState } from 'react'
import { CreatableSelect } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'
import { FormControl, FormLabel, Input } from '@chakra-ui/react'

interface TopicAutoCompleteProps {
  name: string
  isRequired?: boolean
  maxCount?: number
  label: string
  initialRef?: MutableRefObject<null>
  placeholder?: string
}

interface Option {
  readonly label: string
  readonly value: string
}

const TopicAutoComplete: FC<TopicAutoCompleteProps> = ({
  name,
  isRequired,
  initialRef,
  label,
  placeholder,
  maxCount,
}) => {
  const { t } = useTranslation()
  const [inputValue, setInputValue] = useState('')
  const [value, setValue] = useState<readonly Option[]>([])

  const handleKeyDown: KeyboardEventHandler = (event) => {
    if (!inputValue) return
    switch (event.key) {
      case 'Enter':
      case 'Tab':
        setValue((prev) => [...prev, { label: inputValue, value: inputValue }])
        setInputValue('')
        event.preventDefault()
    }
  }

  return (
    <FormControl isRequired={isRequired}>
      <FormLabel>{label}</FormLabel>
      <Input type={'hidden'} name={name} value={value.map((e) => e.label)} />

      <CreatableSelect
        isRequired={isRequired}
        ref={initialRef}
        name={`${name}-internal`}
        components={{
          DropdownIndicator: null,
        }}
        inputValue={inputValue}
        isClearable
        isMulti
        menuIsOpen={false}
        onChange={(newValue) => {
          setValue(newValue)
        }}
        onInputChange={(newValue) => {
          if (maxCount === undefined || value.length < maxCount) setInputValue(newValue)
        }}
        onKeyDown={handleKeyDown}
        placeholder={placeholder || t('bridge.subscription.topic.placeholder')}
        value={value}
      />
    </FormControl>
  )
}

export default TopicAutoComplete
