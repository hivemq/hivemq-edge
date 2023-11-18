export interface MetricInfo {
  device: string
  type: string
  id: string
  suffix: string
}

// TODO[NVL] This is a potentially dangerous approach as there is no guaranties that the pattern will be consistent with other type of adapters.
export const extractMetricInfo = (metricName: string): MetricInfo => {
  const pruneLeading = metricName.replace('com.hivemq.edge.', '')
  const [device, type, id, ...metric] = pruneLeading.split('.')
  const suffix = metric.join('.')

  return { device, type, id, suffix }
}
