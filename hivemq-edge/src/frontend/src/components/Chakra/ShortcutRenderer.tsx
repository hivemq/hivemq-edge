import { FC, Fragment } from 'react'
import { chakra, Kbd, Text } from '@chakra-ui/react'

import i18n from '@/config/i18n.config.ts'
import { getUserAgentPlatform } from '@/utils/user-agent.utils.ts'

interface ShortcutRendererProps {
  hotkeys: string
  description?: string
}

const ShortcutRenderer: FC<ShortcutRendererProps> = ({ hotkeys, description }) => {
  const listHotkeys = hotkeys.split(',')
  const shortcuts = listHotkeys.map((hotkey) => hotkey.split('+'))

  const localiseKeyboard = (shortcut: string[]) => {
    const [modifier, ...rest] = shortcut

    if (modifier === 'Meta') {
      const modifier = i18n.t('shortcuts.modifier.META', { ns: 'components', context: getUserAgentPlatform() })
      return [modifier, ...rest]
    }
    return shortcut
  }

  return (
    <>
      <span role="term" aria-label={hotkeys}>
        {shortcuts.map((shortcut, indexShortcut) => {
          const localisedShortcut = localiseKeyboard(shortcut)
          return (
            <Fragment key={`${shortcut}-${indexShortcut}`}>
              {indexShortcut !== 0 && ' , '}
              {localisedShortcut.map((element, indexElement) => (
                <chakra.span key={`$${shortcut}-${indexShortcut}-${indexElement}`} aria-hidden="true">
                  {indexElement !== 0 && ' + '}
                  <Kbd fontSize="md">{element}</Kbd>
                </chakra.span>
              ))}
            </Fragment>
          )
        })}
      </span>
      {description && (
        <>
          <Text ml={4} as={chakra.span} role="definition">
            {' '}
            {description}
          </Text>
        </>
      )}
    </>
  )
}

export default ShortcutRenderer
