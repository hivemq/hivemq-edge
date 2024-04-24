import { FC, Fragment } from 'react'
import { chakra, Kbd, Text } from '@chakra-ui/react'

interface ShortcutRendererProps {
  hotkeys: string
  description?: string
}

const ShortcutRenderer: FC<ShortcutRendererProps> = ({ hotkeys, description }) => {
  const listHotkeys = hotkeys.split(',')
  const shortcuts = listHotkeys.map((e) => e.split('+'))

  return (
    <>
      <span role="term" aria-label={hotkeys}>
        {shortcuts.map((shortcut, indexShortcut) => (
          <Fragment key={`${shortcut}-${indexShortcut}`}>
            {indexShortcut !== 0 && ' , '}
            {shortcut.map((element, indexElement) => (
              <chakra.span key={`$${shortcut}-${indexShortcut}-${indexElement}`} aria-hidden="true">
                {indexElement !== 0 && ' + '}
                <Kbd>{element}</Kbd>
              </chakra.span>
            ))}
          </Fragment>
        ))}
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
