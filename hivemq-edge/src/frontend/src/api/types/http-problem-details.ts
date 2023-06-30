type extensionProperties = Record<string, unknown>

export interface ProblemDetails {
  detail?: string
  instance?: string
  status: number
  title: string
  type?: string
}

export type ProblemDetailsExtended = ProblemDetails & extensionProperties
