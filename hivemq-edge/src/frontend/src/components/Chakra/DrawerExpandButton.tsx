import type { FC } from 'react'
import type { IconButtonProps } from '@chakra-ui/react'
import { IconButton } from '@chakra-ui/react'
import { LuExpand, LuShrink } from 'react-icons/lu'
import { useTranslation } from 'react-i18next'

interface DrawerExpandButtonProps extends Omit<IconButtonProps, 'aria-label'> {
  isExpanded: boolean
  toggle: () => void
}

const DrawerExpandButton: FC<DrawerExpandButtonProps> = ({ isExpanded, toggle, ...props }) => {
  const { t } = useTranslation('components')
  return (
    <IconButton
      {...props}
      variant="ghost"
      colorScheme="gray"
      onClick={toggle}
      data-expanded={isExpanded}
      icon={isExpanded ? <LuShrink /> : <LuExpand />}
      style={{
        position: 'absolute',
        top: 'var(--chakra-space-2)',
        right: 0,
        width: '32px',
        height: '32px',
        transform: 'translate(-48px, 0)',
        minWidth: 'inherit',
      }}
      aria-label={isExpanded ? t('DrawerExpandButton.aria-label.shrink') : t('DrawerExpandButton.aria-label.expand')}
    />
  )
}

export default DrawerExpandButton
