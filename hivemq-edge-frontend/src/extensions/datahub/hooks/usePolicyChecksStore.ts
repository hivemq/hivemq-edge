import { create } from 'zustand'
import type { Node } from '@xyflow/react'
import type { DryRunResults, PolicyCheckAction, PolicyCheckState } from '@datahub/types.ts'
import { PolicyDryRunStatus } from '@datahub/types.ts'
import type { ProblemDetailsExtended } from '@/api/types/http-problem-details.ts'

const initialStore = (): PolicyCheckState => {
  return {
    node: undefined,
    report: undefined,
    status: PolicyDryRunStatus.IDLE,
  }
}

export const usePolicyChecksStore = create<PolicyCheckState & PolicyCheckAction>()((set, get) => ({
  ...initialStore(),
  reset: () => {
    set(initialStore())
  },
  initReport: () => {
    set({
      report: undefined,
      status: PolicyDryRunStatus.RUNNING,
    })
  },
  setNode: (node: Node | undefined) => {
    set({
      node: node,
    })
  },
  setReport: (report: DryRunResults<unknown, never>[]) => {
    const failedResults = report.filter((result) => !!result.error)
    set({
      report: report,
      status: failedResults.length ? PolicyDryRunStatus.FAILURE : PolicyDryRunStatus.SUCCESS,
    })
  },
  getErrors: () => {
    const report = get().report
    if (!report) return undefined
    const failedResults = report.filter((result) => !!result.error)
    return failedResults.map((result) => result.error as ProblemDetailsExtended)
  },
}))
