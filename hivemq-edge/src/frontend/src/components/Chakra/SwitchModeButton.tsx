import { FC } from 'react'
import { Icon, useColorMode, IconButtonProps } from '@chakra-ui/react'
import { MdDarkMode, MdLightMode } from 'react-icons/md'
import { useTranslation } from 'react-i18next'
import IconButton from '@/components/Chakra/IconButton.tsx'

const SwitchModeButton: FC<Omit<IconButtonProps, 'aria-label'>> = ({ ...props }) => {
  const { t } = useTranslation()
  const { colorMode, toggleColorMode } = useColorMode()

  return (
    <IconButton
      aria-label={t('action.mode', { context: colorMode })}
      onClick={() => toggleColorMode()}
      data-testid={'chakra-ui-switch-mode'}
      icon={<Icon as={colorMode !== 'light' ? MdLightMode : MdDarkMode} boxSize={'24px'} />}
      {...props}
    />
  )
}

export default SwitchModeButton
