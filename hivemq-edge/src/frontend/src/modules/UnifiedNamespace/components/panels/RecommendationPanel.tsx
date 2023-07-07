import { Card, CardBody, CardFooter, CardHeader, Heading, Text } from '@chakra-ui/react'
import NamespaceDisplay from '@/modules/UnifiedNamespace/components/NamespaceDisplay.tsx'
import { useTranslation } from 'react-i18next'

const RecommendationPanel = () => {
  const { t } = useTranslation()

  return (
    <Card>
      <CardHeader>
        <Heading as="h2" size="md">
          {t('unifiedNamespace.container.recommend.title')}
        </Heading>
      </CardHeader>
      <CardBody>
        <Text as={'span'}> {t('unifiedNamespace.container.recommend.by')}</Text>{' '}
        <Text as={'span'} fontSize="2xl">
          {t('unifiedNamespace.standard')}
        </Text>
      </CardBody>
      <CardFooter>
        <NamespaceDisplay
          namespace={{
            enterprise: 'Enterprise',
            site: 'Site',
            area: 'Area',
            productionLine: 'Line',
            workCell: 'Cell',
          }}
        />
      </CardFooter>
    </Card>
  )
}

export default RecommendationPanel
