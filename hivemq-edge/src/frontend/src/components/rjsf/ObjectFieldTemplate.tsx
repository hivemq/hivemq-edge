import type { FormContextType, ObjectFieldTemplateProps, RJSFSchema, StrictRJSFSchema } from '@rjsf/utils'
import { getTemplate, getUiOptions, titleId } from '@rjsf/utils'
import { Box, Tab, TabList, TabPanel, TabPanels, Tabs } from '@chakra-ui/react'
import type { UITab } from '@/components/rjsf/Form/types.ts'
import { useFormControlStore } from '@/components/rjsf/Form/useFormControlStore.ts'

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
  const { tabIndex, setTabIndex } = useFormControlStore()

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
      <Tabs index={tabIndex} onChange={setTabIndex}>
        <TabList>
          {tabs.map((e) => {
            const filteredProps = properties.filter((property) => e.properties.includes(property.name))
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
            const filteredProps = properties.filter((property) => e.properties.includes(property.name))
            if (!filteredProps.length) return null
            return (
              <TabPanel key={e.id} p={0} pt="1px" mb={6}>
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
          .filter((property) => !allGrouped.includes(property.name))
          .map((prop) => (
            <Box _first={{ marginTop: '24px' }} _notLast={{ marginBottom: '24px' }} key={prop.content.key}>
              {prop.content}
            </Box>
          ))}
      </Tabs>
    </>
  )
}
