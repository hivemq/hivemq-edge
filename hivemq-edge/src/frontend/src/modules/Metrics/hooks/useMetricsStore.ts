import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

import { ChartType } from '../types.ts'

export interface MetricDefinitionSpec {
  source: string
  metrics: string
  chart?: ChartType
}

export interface MetricDefinitionStore {
  metrics: MetricDefinitionSpec[]
  reset: () => void
  addMetrics: (source: string, metrics: string, chart?: ChartType) => void
  removeMetrics: (source: string, metrics: string) => void
  getMetricsFor: (source: string) => MetricDefinitionSpec[]
}

const useMetricsStore = create<MetricDefinitionStore>()(
  persist(
    (set, get) => ({
      metrics: [],
      reset: () => {
        set({ metrics: [] })
      },
      addMetrics: (source: string, metrics, chart) => {
        set({
          metrics: [...get().metrics, { source: source, metrics: metrics, chart: chart }],
        })
      },
      removeMetrics: (source: string, metrics: string) => {
        set({
          metrics: get().metrics.filter((e) => e.source !== source || e.metrics !== metrics),
        })
      },
      getMetricsFor: (source: string) => {
        return get().metrics.filter((m) => m.source === source)
      },
    }),
    {
      name: 'edge.observability',
      storage: createJSONStorage(() => localStorage),
    }
  )
)

export default useMetricsStore
