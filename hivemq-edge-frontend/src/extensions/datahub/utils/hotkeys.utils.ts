/**
 * Adapted from https://github.com/JohannesKlauss/react-hotkeys-hook/blob/main/src/parseHotkeys.ts
 */

const reservedModifierKeywords = ['shift', 'alt', 'meta', 'mod', 'ctrl']

const mappedKeys: Record<string, string> = {
  esc: 'escape',
  return: 'enter',
  '.': 'period',
  ',': 'comma',
  '-': 'slash',
  ' ': 'space',
  '`': 'backquote',
  '#': 'backslash',
  '+': 'bracketright',
  ShiftLeft: 'shift',
  ShiftRight: 'shift',
  AltLeft: 'alt',
  AltRight: 'alt',
  MetaLeft: 'meta',
  MetaRight: 'meta',
  OSLeft: 'meta',
  OSRight: 'meta',
  ControlLeft: 'ctrl',
  ControlRight: 'ctrl',
}

function mapKey(key: string): string {
  return (mappedKeys[key] || key)
    .trim()
    .toLowerCase()
    .replace(/key|digit|numpad|arrow/, '')
}

export function parseHotkey(hotkey: string, combinationKey = '+'): KeyboardEventInit {
  const keys = hotkey
    .toLocaleLowerCase()
    .split(combinationKey)
    .map((k) => mapKey(k))

  const modifiers: EventModifierInit = {
    altKey: keys.includes('alt'),
    ctrlKey: keys.includes('ctrl') || keys.includes('control'),
    shiftKey: keys.includes('shift'),
    metaKey: keys.includes('meta'),
    modifierFn: keys.includes('mod'),
  }

  const singleCharKeys = keys.filter((k) => !reservedModifierKeywords.includes(k))
  return {
    ...modifiers,
    // Not supporting combined Hot Key. Take only the first one
    key: singleCharKeys[0],
  }
}
