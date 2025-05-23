import GenericProvider from './genericProvider.js';
import AutomateProvider from './automateProvider.js';
export default class ProviderResolver {
  static resolve(sessionId, commandExecutorUrl, capabilities, sessionCapabilities, clientInfo, environmentInfo, options, buildInfo) {
    // We can safely do [0] because GenericProvider is catch all
    const Klass = [AutomateProvider, GenericProvider].filter(provider => provider.supports(commandExecutorUrl))[0];
    const args = {
      sessionId,
      commandExecutorUrl,
      capabilities,
      sessionCapabilities,
      clientInfo,
      environmentInfo,
      options,
      buildInfo
    };
    return new Klass(args);
  }
}