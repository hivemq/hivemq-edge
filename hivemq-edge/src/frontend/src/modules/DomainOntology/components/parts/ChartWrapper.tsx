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
  Box,
  CardFooter,
} from '@chakra-ui/react'

interface ChartWrapperProps {
  children: React.ReactNode
  help?: React.ReactNode
  helpTitle?: string
  cta?: React.ReactNode
  footer?: React.ReactNode
}

const ChartWrapper: FC<ChartWrapperProps> = ({ children, cta, help, helpTitle, footer, ...props }) => {
  return (
    <Card size="sm" {...props}>
      {(cta || help) && (
        <CardHeader as={HStack} justifyContent={cta && help ? 'space-between' : cta ? 'flex-start' : 'flex-end'}>
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
      <CardBody>
        <Box w="100%" h="60vh">
          {children}
        </Box>
      </CardBody>
      {footer && <CardFooter>{footer}</CardFooter>}
    </Card>
  )
}

export default ChartWrapper
