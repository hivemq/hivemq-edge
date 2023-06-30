import { ApiBearerToken, UsernamePasswordCredentials } from '../../../__generated__'
// @ts-ignore an import is not working
import { CyHttpMessages } from 'cypress/types/net-stubbing'
import { rest } from 'msw'

const TOKEN =
  'eyJraWQiOiIwMDAwMSIsImFsZyI6IlJTMjU2In0.' +
  'eyJqdGkiOiI2TVExbWtPeFRqUmdFMS1BRE9GNGRRIiwiaWF0IjoxNjg1MDE3OTY3LCJhdWQiOiJIaXZlTVEtRWRnZS1BcGkiLCJpc3MiOiJIaXZlTVEtRWRnZSIsImV4cCI6MTY4NTAxOTc2NywibmJmIjoxNjg1MDE3ODQ3LCJzdWIiOiJhZG1pbiIsInJvbGVzIjpbImFkbWluIl19.' +
  'o4jlGf7dDBOmfkr46u_MTQA-C3I2ZkHyrljbfNPeqax_EI54vigHH4viVk0yehH47BivbGLp9CmsTujre-G7hrFHliXsbZ81erJ3IgWpSwq1GZdvEFynVabm26QO8fKm3CnpwJDIfPYKOuTEXYpWlDe2pc9_r6oJSllb1UP30SP4UVxC9rIUm4zGbAVNFizaqLo_V92cLW2pv0T2SChJK6pDYSj19RWhoftEES9G-hd8b--_eB2AkflUseUlDDIsGMxQxTdMfR4l6q2C1RNiGdub3KVpnHN86sF_u8zQb4QdmtcXqS4imm1BiZZm67hZQYtX12cLy6sf11Dh8b09Gg'

export const mockValidCredentials: UsernamePasswordCredentials = {
  userName: 'admin',
  password: 'password',
}

export const mockAuthApi = (credentials: UsernamePasswordCredentials) => {
  if (credentials.userName === 'admin' && credentials.password === 'password')
    return (req: CyHttpMessages.IncomingHttpRequest) => {
      req.reply({
        token: TOKEN,
      })
    }
  return (req: CyHttpMessages.IncomingHttpRequest) => {
    req.reply({ statusCode: 401, status: 401, body: { title: 'Invalid username and/or password', code: 401 } })
  }
}

export const handlers = [
  rest.post('*/auth/authenticate', (_, res, ctx) => {
    return res(
      ctx.json<ApiBearerToken>({
        token: TOKEN,
      }),

      ctx.status(200)
    )
  }),
]
