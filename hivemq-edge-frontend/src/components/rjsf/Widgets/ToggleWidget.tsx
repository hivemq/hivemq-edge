import type React from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { Switch, FormControl, FormLabel } from '@chakra-ui/react'

const ToggleWidget: React.FC<WidgetProps> = ({ id, value, required, disabled, readonly, label, onChange }) => {
  console.log('XXXXXXXXXX')
  return (
    <FormControl display="flex" alignItems="center" isRequired={required} isDisabled={disabled || readonly}>
      <FormLabel htmlFor={id} mb="0">
        {label}
      </FormLabel>
      <Switch
        id={id}
        isChecked={!!value}
        onChange={(e) => onChange(e.target.checked)}
        isDisabled={disabled || readonly}
      />
    </FormControl>
  )
}

export default ToggleWidget
