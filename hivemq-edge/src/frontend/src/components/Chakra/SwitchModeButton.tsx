import { FC } from 'react'
import { Icon, IconButton, useColorMode, IconButtonProps } from '@chakra-ui/react'
import { MdDarkMode, MdLightMode } from 'react-icons/md'
import { useTranslation } from 'react-i18next'

const SwitchModeButton: FC<Omit<IconButtonProps, 'aria-label'>> = ({ ...props }) => {
  const { t } = useTranslation()
  const { colorMode, toggleColorMode } = useColorMode()

  return (
    <IconButton
      colorScheme={'brand'}
      aria-label={t('action.mode', { context: colorMode })}
      size={'sm'}
      onClick={() => toggleColorMode()}
      data-testid={'chakra-ui-switch-mode'}
      icon={<Icon as={colorMode !== 'light' ? MdLightMode : MdDarkMode} boxSize={'24px'} />}
      {...props}
    />
  )
}

export default SwitchModeButton
