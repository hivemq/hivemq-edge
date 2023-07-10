import { useTranslation } from 'react-i18next'
import { Box, Card, CardBody, CardHeader, Flex, Heading, Text } from '@chakra-ui/react'

import NamespaceDisplay from '@/modules/UnifiedNamespace/components/NamespaceDisplay.tsx'

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
        <Flex flexDirection={'column'}>
          <Box>
            <Text as={'span'}> {t('unifiedNamespace.container.recommend.by')}</Text>{' '}
            <Text as={'span'} fontSize="2xl">
              {t('unifiedNamespace.standard')}
            </Text>
          </Box>
          <NamespaceDisplay
            namespace={{
              enterprise: 'Enterprise',
              site: 'Site',
              area: 'Area',
              productionLine: 'Line',
              workCell: 'Cell',
            }}
          />
        </Flex>
      </CardBody>
    </Card>
  )
}

export default RecommendationPanel
