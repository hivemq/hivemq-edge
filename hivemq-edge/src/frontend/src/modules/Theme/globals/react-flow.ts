import type { SystemStyleObject } from '@chakra-ui/react'

export const reactFlow: SystemStyleObject = {
  '.react-flow__node.selectable': {
    '&:focus-visible': {
      boxShadow: '0 0 10px 2px rgb(88 144 255 / 75%), 0 1px 1px rgb(0 0 0 / 15%)',
    },
  },

  '.connectingto': {
    '&.valid': {
      boxShadow: '0 0 10px 2px rgb(88 144 255 / 75%), 0 1px 1px rgb(0 0 0 / 15%);',
    },
    '&:not(.valid)': {
      cursor: 'no-drop',
      boxShadow: '0 0 10px 2px rgba(226, 85, 85, 0.75), 0 1px 1px rgb(0 0 0 / 15%);',
    },
    '&:not(.connectable)': {
      cursor: 'no-drop;',
      boxShadow: '0 0 10px 2px rgba(226, 85, 85, 0.75), 0 1px 1px rgb(0 0 0 / 15%);',
    },
  },

  '.react-flow__node': {
    '.react-flow__handle.onSuccess': {
      backgroundColor: 'green.400',
    },
    '.react-flow__handle.onError': {
      backgroundColor: 'red.400',
    },
  },
}
