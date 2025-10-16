import type { FC, PropsWithChildren } from 'react'
import { Card, CardBody, CardHeader, Switch, FormControl } from '@chakra-ui/react'

interface WrapperFilterProps {
  id: string
  label: string
  isActive: boolean
  onChange?: (value: boolean) => void
}

const WrapperCriteria: FC<PropsWithChildren<WrapperFilterProps>> = ({ id, isActive, label, children, onChange }) => {
  return (
    <Card
      size="sm"
      alignContent="space-between"
      width="-webkit-fill-available"
      data-testid={`workspace-filter-${id}-container`}
    >
      <CardHeader>
        <FormControl>
          <Switch
            id={`workspace-filter-${id}-switch`}
            onChange={(e) => onChange?.(e.target.checked)}
            isChecked={isActive}
          >
            {label}
          </Switch>
        </FormControl>
      </CardHeader>
      <CardBody data-testid={`workspace-filter-${id}-control`}>{children}</CardBody>
    </Card>
  )
}

export default WrapperCriteria
