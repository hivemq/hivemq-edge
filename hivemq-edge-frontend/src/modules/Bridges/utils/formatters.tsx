export const leadSHA = (sha: string, length: number): string => {
  return sha.slice(0, length)
}

export const formatHost = (host: string | undefined, length = 7): string => {
  if (!host) return ''

  const [domain, ...rest] = host.split('.')
  const reducedHost = leadSHA(domain, length)
  return [reducedHost === domain ? reducedHost : reducedHost + '[...]', ...rest].join('.')
}
