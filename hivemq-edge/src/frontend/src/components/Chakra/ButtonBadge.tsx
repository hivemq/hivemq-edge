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
  return (
    <Avatar
      as={IconButton}
      onClick={onClick}
      aria-label={ariaLabel}
      isDisabled={isDisabled}
      icon={icon}
      sx={{
        borderColor: 'brand.500',
        backgroundColor: 'brand.500',
        _disabled: {
          backgroundColor: 'brand.200',
          borderColor: 'brand.200',
        },
        _hover: {
          backgroundColor: 'brand.700',
          _disabled: {
            backgroundColor: 'brand.200',
            borderColor: 'brand.200',
          },
        },
        _active: { backgroundColor: 'brand.900' },
        _dark: {
          borderColor: 'brand.200',
          backgroundColor: 'brand.200',
          _hover: { backgroundColor: 'brand.300' },
          _active: { backgroundColor: 'brand.400' },
        },
      }}
      {...props}
    >
      {((badgeCount && !isDisabled) || !isDisabled) && (
        <AvatarBadge borderColor="yellow.500" bg="yellow.500" boxSize="1.25em" data-testid={'buttonBadge-counter'}>
          <Text fontSize={'.95rem'}>{badgeCount}</Text>
        </AvatarBadge>
      )}
    </Avatar>
  )
}

export default ButtonBadge
