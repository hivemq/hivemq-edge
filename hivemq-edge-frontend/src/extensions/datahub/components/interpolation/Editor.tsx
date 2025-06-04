import type { FC } from 'react'
import { EditorContent, useEditor } from '@tiptap/react'
import Mention from '@tiptap/extension-mention'
import Text from '@tiptap/extension-text'
import Document from '@tiptap/extension-document'
import Paragraph from '@tiptap/extension-paragraph'
import Placeholder from '@tiptap/extension-placeholder'

import { Box, chakra, Spinner, useColorModeValue } from '@chakra-ui/react'

import { Suggestion } from '@datahub/components/interpolation/Suggestion.ts'
import { parseInterpolations } from '@datahub/components/interpolation/interpolation.utils.ts'

interface EditorProps {
  as?: 'input' | 'textarea'
  id: string
  labelId: string
  isRequired?: boolean
  readonly?: boolean
  placeholder?: string
  value: string | undefined
  onChange?: (value: string) => void
  isInvalid?: boolean
}

const StyledEditor = chakra(EditorContent)
const SingleLineDocument = Document.extend({
  content: 'block',
})

export const Editor: FC<EditorProps> = ({
  as = 'input',
  id,
  labelId,
  isRequired,
  readonly,
  onChange,
  value,
  placeholder,
  isInvalid,
}) => {
  const bgMention = useColorModeValue('gray.100', 'rgba(226, 232, 240, 0.16)')
  const colorMention = useColorModeValue('gray.800', 'gray.200')

  const editor = useEditor({
    onUpdate: ({ editor }) => onChange?.(editor.getText()),
    extensions: [
      as === 'input' ? SingleLineDocument : Document,
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

  editor?.setEditable(!readonly)
  const focus = () => editor?.chain().focus()

  if (!editor) {
    return <Spinner />
  }

  return (
    <Box
      pt={2}
      pb={4}
      w="full"
      onClick={focus}
      borderRadius="md"
      border="1px solid"
      borderColor={isInvalid ? 'red.500' : 'gray.200'}
      boxShadow={isInvalid ? 'invalidInput' : 'none'}
      sx={{
        opacity: readonly ? '0.4' : undefined,
        '.mention': {
          backgroundColor: bgMention,
          color: colorMention,
          padding: '5px',
          userSelect: 'none',
          borderRadius: '2px',
        },
        '.tiptap p.is-editor-empty:first-child::before': {
          color: '#adb5bd',
          content: `attr(data-placeholder)`,
          float: 'left',
          height: 0,
          pointerEvents: 'none',
        },
      }}
    >
      <StyledEditor
        role="textbox"
        aria-required={isRequired}
        aria-multiline={as === 'textarea'}
        aria-autocomplete="list"
        aria-placeholder={placeholder}
        aria-labelledby={labelId}
        editor={editor}
        id={id}
        sx={{ minH: '36', pl: 4, '& :focus-visible': { outline: '0px' } }}
      />
    </Box>
  )
}
