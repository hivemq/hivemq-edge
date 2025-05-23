import ProviderResolver from './providers/providerResolver.js';
import utils from '@percy/sdk-utils';
import PlaywrightProvider from './providers/playwrightProvider.js';
export default class WebdriverUtils {
  static async captureScreenshot({
    sessionId,
    commandExecutorUrl,
    capabilities,
    sessionCapabilities,
    frameGuid,
    pageGuid,
    framework,
    snapshotName,
    clientInfo,
    environmentInfo,
    options = {},
    buildInfo = {}
  }) {
    const log = utils.logger('webdriver-utils:captureScreenshot');
    try {
      const startTime = Date.now();
      log.info(`[${snapshotName}] : Starting automate screenshot capture ...`);
      let provider;
      switch (framework ? framework.toLowerCase() : null) {
        case 'playwright':
          provider = new PlaywrightProvider(sessionId, frameGuid, pageGuid, clientInfo, environmentInfo, options, buildInfo);
          break;
        default:
          provider = ProviderResolver.resolve(sessionId, commandExecutorUrl, capabilities, sessionCapabilities, clientInfo, environmentInfo, options, buildInfo);
      }
      await provider.createDriver();
      log.debug(`[${snapshotName}] : Created driver ...`);
      const comparisonData = await provider.screenshot(snapshotName, options);
      comparisonData.metadata.cliScreenshotStartTime = startTime;
      comparisonData.metadata.cliScreenshotEndTime = Date.now();
      comparisonData.sync = options.sync;
      comparisonData.testCase = options.testCase;
      comparisonData.thTestCaseExecutionId = options.thTestCaseExecutionId;
      log.debug(`[${snapshotName}] : Comparison Data: ${JSON.stringify(comparisonData)}`);
      return comparisonData;
    } catch (e) {
      log.error(`[${snapshotName}] : Error - ${e.message}`);
      log.error(`[${snapshotName}] : Error Log - ${e.toString()}`);
      throw e; // Re-throw the error to maintain consistency in error handling
    }
  }
}