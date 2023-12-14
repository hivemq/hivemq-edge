import { Icon, type IconProps } from '@chakra-ui/react'
import { FC } from 'react'
import { PiGraphFill } from 'react-icons/pi'

const WorkspaceIcon: FC<IconProps> = (props) => {
  return <Icon as={PiGraphFill} boxSize={6} {...props} />
}

export default WorkspaceIcon
