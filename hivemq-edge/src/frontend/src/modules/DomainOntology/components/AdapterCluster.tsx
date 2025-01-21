import { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { HierarchyNode } from 'd3-hierarchy'
import { Box, Text } from '@chakra-ui/react'

import TreeChart from '@/modules/DomainOntology/components/charts/TreeChart.tsx'
import ConfigurationPanel from '@/modules/DomainOntology/components/cluster/ConfigurationPanel.tsx'
import { useGetCluster } from '@/modules/DomainOntology/hooks/useGetCluster.ts'
import { ClusterDataWrapper } from '@/modules/DomainOntology/utils/cluster.utils.ts'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'

const AdapterCluster: FC = () => {
  const { t } = useTranslation()
  const { data, clusterKeys, setClusterKeys } = useGetCluster()

  const arrayHelpStrings = t('ontology.charts.cluster.help', { returnObjects: true }) as unknown as string[]

  return (
    <ChartWrapper
      cta={<ConfigurationPanel groupKeys={clusterKeys} onSubmit={(a) => setClusterKeys(a)} />}
      helpTitle={t('ontology.charts.cluster.title')}
      help={arrayHelpStrings.map((line, index) => (
        <Text key={`test-${index}`}>{line}</Text>
      ))}
    >
      <Box w="100%" h="69vh">
        <TreeChart
          data={data}
          identity={(e) => {
            const node = e as HierarchyNode<ClusterDataWrapper>
            if (node.depth === 0) return 'Hive MQ'
            if (Array.isArray(node.data)) {
              const [key] = node.data
              return key || 'No data'
            }
            if (node.data) return node.data.name

            return node.name
          }}
        />
      </Box>
    </ChartWrapper>
  )
}

export default AdapterCluster
