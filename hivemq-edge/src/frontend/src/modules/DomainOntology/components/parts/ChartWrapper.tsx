import { FC } from 'react'
import {
  Card,
  CardBody,
  CardHeader,
  Popover,
  PopoverTrigger,
  PopoverContent,
  PopoverHeader,
  PopoverBody,
  PopoverArrow,
  PopoverCloseButton,
  HStack,
  Button,
} from '@chakra-ui/react'

interface ChartWrapperProps {
  id?: string
  children: React.ReactNode
  help?: React.ReactNode
  helpTitle?: string
  cta?: React.ReactNode
}

const ChartWrapper: FC<ChartWrapperProps> = ({ children, cta, help, helpTitle }) => {
  return (
    <Card size="sm">
      {(cta || help) && (
        <CardHeader as={HStack} justifyContent="space-between">
          {cta && cta}
          {help && (
            <Popover placement="bottom-end">
              <PopoverTrigger>
                <Button>Help</Button>
              </PopoverTrigger>
              <PopoverContent>
                <PopoverArrow />
                <PopoverCloseButton />
                <PopoverHeader>{helpTitle || 'About the visualisation'}</PopoverHeader>
                <PopoverBody>{help}</PopoverBody>
              </PopoverContent>
            </Popover>
          )}
        </CardHeader>
      )}
      <CardBody>{children}</CardBody>
    </Card>
  )
}

export default ChartWrapper
