import { useEditor, EditorContent } from '@tiptap/react'
import Mention from '@tiptap/extension-mention'
import Text from '@tiptap/extension-text'
import Document from '@tiptap/extension-document'
import Paragraph from '@tiptap/extension-paragraph'
import { Box } from '@chakra-ui/react'
import { Suggestion } from '@datahub/components/interpolation/Suggestion.ts'

export const Editor = () => {
  const editor = useEditor({
    onUpdate: (e) => console.log('DSSDSSS', e.editor.getText(), e.editor.getJSON()),
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
    ],
    content: `
       The topic <span data-type="mention" data-id="topicId"></span> cannot be resolved. Check the <span data-type="mention" data-id="policyId"></span>
        and try again later. We are dropping the topic anyway

      `,
  })

  if (!editor) {
    return null
  }

  return (
    <Box
      p={2}
      sx={{ '.mention': { backgroundColor: 'red.100', padding: '5px', userSelect: 'none', borderRadius: '2px' } }}
    >
      <EditorContent editor={editor} />
    </Box>
  )
}
