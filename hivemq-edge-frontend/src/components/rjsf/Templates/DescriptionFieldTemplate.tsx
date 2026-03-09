import type {
  DescriptionFieldProps,
  FormContextType,
  GenericObjectType,
  RJSFSchema,
  StrictRJSFSchema,
} from '@rjsf/utils'
import { Box } from '@chakra-ui/react'
import ReactMarkdown from 'react-markdown'

export const DescriptionFieldTemplate = <
  T = unknown,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = GenericObjectType,
>({
  description,
  id,
}: DescriptionFieldProps<T, S, F>) => {
  if (!description) {
    return null
  }

  if (typeof description === 'string') {
    return (
      <Box id={id} fontSize="sm" mt={2} mb={4}>
        <ReactMarkdown
          components={{
            p: ({ children }) => <p style={{ marginBottom: '0.5em' }}>{children}</p>,
            a: ({ href, children }) => (
              <a href={href} target="_blank" rel="noopener noreferrer" style={{ color: 'inherit', textDecoration: 'underline' }}>
                {children}
              </a>
            ),
          }}
        >
          {description}
        </ReactMarkdown>
      </Box>
    )
  }

  return description
}
