import {
  FormContextType,
  getTemplate,
  getUiOptions,
  ObjectFieldTemplateProps,
  RJSFSchema,
  StrictRJSFSchema,
  titleId,
} from '@rjsf/utils'
import { Box, Tab, TabList, TabPanel, TabPanels, Tabs } from '@chakra-ui/react'
import { UITab } from '@/modules/ProtocolAdapters/types.ts'

export const ObjectFieldTemplate = <
  T = never,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = never
>(
  props: ObjectFieldTemplateProps<T, S, F>
) => {
  const { registry, properties, title, description, uiSchema, required, schema, idSchema } = props
  const options = getUiOptions<T, S, F>(uiSchema)
  const TitleFieldTemplate = getTemplate<'TitleFieldTemplate', T, S, F>('TitleFieldTemplate', registry, options)

  // @ts-ignore Type will need to be corrected
  const { tabs }: { tabs: UITab[] } = options
  if (!tabs) {
    return (
      <Box>
        {title && (
          <TitleFieldTemplate
            id={titleId<T>(idSchema)}
            title={title}
            required={required}
            schema={schema}
            uiSchema={uiSchema}
            registry={registry}
          />
        )}
        {description}
        {properties.map((prop) => (
          <Box _notLast={{ marginBottom: '24px' }} className="x0" key={prop.content.key}>
            {prop.content}
          </Box>
        ))}
      </Box>
    )
  }

  // TODO[NVL] Not efficient. Build a cluster
  const allGrouped = tabs.map((e) => e.properties).flat()

  return (
    <>
      <Tabs>
        <TabList>
          {tabs.map((e) => {
            const filteredProps = properties.filter((p) => e.properties.includes(p.name))
            if (!filteredProps.length) return null
            return (
              <Tab fontSize="md" key={e.id}>
                {e.title}
              </Tab>
            )
          })}
        </TabList>

        <TabPanels>
          {tabs.map((e) => {
            const filteredProps = properties.filter((p) => e.properties.includes(p.name))
            if (!filteredProps.length) return null
            return (
              <TabPanel key={e.id} p={0} pt={'1px'} mb={6}>
                <>
                  {filteredProps.map((prop) => (
                    <Box _first={{ marginTop: '24px' }} _notLast={{ marginBottom: '24px' }} key={prop.content.key}>
                      {prop.content}
                    </Box>
                  ))}
                </>
              </TabPanel>
            )
          })}
        </TabPanels>
        {properties
          .filter((e) => !allGrouped.includes(e.name))
          .map((prop) => (
            <Box _first={{ marginTop: '24px' }} _notLast={{ marginBottom: '24px' }} key={prop.content.key}>
              {prop.content}
            </Box>
          ))}
      </Tabs>
    </>
  )
}
