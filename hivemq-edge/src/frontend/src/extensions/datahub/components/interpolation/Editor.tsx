import { FC } from 'react'
import { EditorContent, useEditor } from '@tiptap/react'
import Mention from '@tiptap/extension-mention'
import Text from '@tiptap/extension-text'
import Document from '@tiptap/extension-document'
import Paragraph from '@tiptap/extension-paragraph'
import Placeholder from '@tiptap/extension-placeholder'

import { Box } from '@chakra-ui/react'

import { Suggestion } from '@datahub/components/interpolation/Suggestion.ts'
import { parseInterpolations } from '@datahub/components/interpolation/interpolation.utils.ts'

interface EditorProps {
  id: string
  isRequired?: boolean
  placeholder?: string
  value: string | undefined
  onChange?: (value: string) => void
}

export const Editor: FC<EditorProps> = ({ id, onChange, value, placeholder }) => {
  const editor = useEditor({
    onUpdate: (e) => {
      onChange?.(e.editor.getText())
    },
    extensions: [
      Document,
      Paragraph,
      Text,
      Mention.configure({
        suggestion: Suggestion,
        HTMLAttributes: {
          class: 'mention',
        },
        renderText: ({ node }) => `$\{${node.attrs.label ?? node.attrs.id}}`,
      }),
      Placeholder.configure({
        placeholder: placeholder,
      }),
    ],
    content: parseInterpolations(value),
    injectCSS: false,
  })

  if (!editor) {
    return null
  }

  return (
    <Box
      sx={{
        '.mention': { backgroundColor: 'red.100', padding: '5px', userSelect: 'none', borderRadius: '2px' },
        '.tiptap p.is-editor-empty:first-child::before': {
          color: '#adb5bd',
          content: `attr(data-placeholder)`,
          float: 'left',
          height: 0,
          pointerEvents: 'none',
        },
      }}
    >
      <EditorContent editor={editor} id={id} />
    </Box>
  )
}
