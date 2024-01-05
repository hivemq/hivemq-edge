import { FC } from 'react'
import { Alert, AlertIcon, AlertStatus, type AlertProps } from '@chakra-ui/react'
import { Event } from '@/api/__generated__'

interface SeverityBadgeProps extends AlertProps {
  event: Event
}

const SeverityBadge: FC<SeverityBadgeProps> = ({ event, ...alertProps }) => {
  let status: AlertStatus = 'info'
  switch (event.severity) {
    case Event.severity.CRITICAL:
      status = 'error'
      break
    case Event.severity.ERROR:
      status = 'error'
      break
    case Event.severity.WARN:
      status = 'warning'
      break
    case Event.severity.INFO:
      status = 'info'
      break
  }

  return (
    <Alert status={status} {...alertProps} addRole={false} px={2} py={'2px'} borderRadius={'15px'}>
      <AlertIcon />
      {event.severity}
    </Alert>
  )
}

export default SeverityBadge
