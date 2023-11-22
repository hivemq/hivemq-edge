import { FC } from 'react'
import { Avatar, AvatarBadge, AvatarProps, IconButton, Text } from '@chakra-ui/react'

interface ButtonBadgeProps extends Omit<AvatarProps, 'aria-label'> {
  badgeCount?: number
  isDisabled?: boolean
  'aria-label': string
}

const ButtonBadge: FC<ButtonBadgeProps> = ({
  ['aria-label']: ariaLabel,
  badgeCount,
  icon,
  isDisabled,
  onClick,
  ...props
}) => {
  // TODO[NVL] Not a good idea. Keep the button but disabled ?
  const activeProps: Partial<Omit<AvatarProps, 'aria-label'>> = isDisabled
    ? { backgroundColor: 'brand.200' }
    : {
        backgroundColor: 'brand.500',
        as: IconButton,
        _hover: { backgroundColor: 'brand.500' },
        _active: { backgroundColor: 'brand.600' },
        onClick: onClick,
      }

  return (
    <Avatar aria-label={ariaLabel} isDisabled={isDisabled} icon={icon} bg="brand.400" {...activeProps} {...props}>
      {((badgeCount && !isDisabled) || !isDisabled) && (
        <AvatarBadge borderColor="yellow.50" bg="yellow.500" boxSize="1.25em">
          <Text fontSize={'.95rem'}>{badgeCount}</Text>
        </AvatarBadge>
      )}
    </Avatar>
  )
}

export default ButtonBadge
