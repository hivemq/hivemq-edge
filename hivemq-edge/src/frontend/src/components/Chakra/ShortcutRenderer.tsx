import { FC, Fragment } from 'react'
import { chakra, Kbd, Text } from '@chakra-ui/react'

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
      const os = window.navigator.platform
      if (os.startsWith('Mac')) {
        return ['Command', ...rest]
      } else {
        return ['Ctrl', ...rest]
      }
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
                  <Kbd>{element}</Kbd>
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
