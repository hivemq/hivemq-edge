// overriding the heading definition
import { RJSFSchema, StrictRJSFSchema, TitleFieldProps } from '@rjsf/utils'
import { Box, Divider, Heading } from '@chakra-ui/react'

export const TitleFieldTemplate = <T = unknown, S extends StrictRJSFSchema = RJSFSchema>({
  id,
  title,
}: TitleFieldProps<T, S>) => (
  <Box id={id} mt={1} mb={4}>
    <Heading as="h2" size="md">
      {title}
    </Heading>
    <Divider />
  </Box>
)
