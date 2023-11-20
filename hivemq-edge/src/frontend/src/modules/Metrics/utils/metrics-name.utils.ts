export interface MetricInfo {
  device: string
  type?: string
  id?: string
  suffix: string
}

// TODO[NVL] This is a potentially dangerous approach as there is no guaranties that the pattern will be consistent with other type of adapters.
export const extractMetricInfo = (metricName: string): MetricInfo => {
  const pruneLeading = metricName.replace('com.hivemq.edge.', '')
  const splitName = pruneLeading.split('.')
  if (splitName[0] === 'bridge') {
    const [device, id, ...metric] = splitName
    const suffix = metric.join('.')
    return { device, id, suffix }
  }
  if (splitName[0] === 'protocol-adapters') {
    const [device, type, id, ...metric] = pruneLeading.split('.')
    const suffix = metric.join('.')
    return { device, type, id, suffix }
  }
  if (['networking', 'messages'].includes(splitName[0])) {
    const [device, ...metric] = pruneLeading.split('.')
    const suffix = metric.join('.')
    return { device, suffix }
  }
  const [device, type, id, ...metric] = pruneLeading.split('.')
  const suffix = metric.join('.')

  return { device, type, id, suffix }
}
