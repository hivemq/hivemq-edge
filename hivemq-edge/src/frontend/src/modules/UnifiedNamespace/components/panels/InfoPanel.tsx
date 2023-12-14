import config from '@/config'
import { Button, Card, CardBody, CardFooter, CardHeader, Heading, Text } from '@chakra-ui/react'
import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { GoLinkExternal } from 'react-icons/go'
import { Link as RouterLink } from 'react-router-dom'

const InfoPanel: FC = () => {
  const { t } = useTranslation()
  const descriptions = t('unifiedNamespace.container.info.descriptions', {
    returnObjects: true,
  }) as string[]

  return (
    <Card>
      <CardHeader>
        <Heading as="h2" size="md">
          {t('unifiedNamespace.container.info.title')}
        </Heading>
      </CardHeader>
      <CardBody>
        {descriptions.map((e, i) => (
          <Text key={`description-${i}`}>{e}</Text>
        ))}
      </CardBody>
      <CardFooter>
        <Button
          variant="link"
          as={RouterLink}
          to={config.documentation.namespace}
          target={'hivemq:docs'}
          aria-label={t('unifiedNamespace.container.info.link') as string}
          leftIcon={<GoLinkExternal />}
          data-testid={'namespace-info-documentation'}
          size="lg"
        >
          {t('unifiedNamespace.container.info.link') as string}
        </Button>
      </CardFooter>
    </Card>
  )
}

export default InfoPanel
