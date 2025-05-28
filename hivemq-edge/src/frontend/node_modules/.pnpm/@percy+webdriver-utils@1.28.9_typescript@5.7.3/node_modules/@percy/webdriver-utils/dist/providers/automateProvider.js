import utils from '@percy/sdk-utils';
import GenericProvider from './genericProvider.js';
import Cache from '../util/cache.js';
import Tile from '../util/tile.js';
import TimeIt from '../util/timing.js';
import MetaDataResolver from '../metadata/metaDataResolver.js';
import Driver from '../driver.js';
const log = utils.logger('webdriver-utils:automateProvider');
export default class AutomateProvider extends GenericProvider {
  static supports(commandExecutorUrl) {
    return commandExecutorUrl.includes(process.env.AA_DOMAIN || 'browserstack');
  }
  async createDriver() {
    this.driver = new Driver(this.sessionId, this.commandExecutorUrl, this.capabilities);
    log.debug(`Passed capabilities -> ${JSON.stringify(this.capabilities)}`);
    const caps = await this.driver.getCapabilites();
    log.debug(`Fetched capabilities -> ${JSON.stringify(caps)}`);
    this.metaData = MetaDataResolver.resolve(this.driver, caps, this.capabilities);
  }
  async screenshot(name, {
    ignoreRegionXpaths = [],
    ignoreRegionSelectors = [],
    ignoreRegionElements = [],
    customIgnoreRegions = [],
    considerRegionXpaths = [],
    considerRegionSelectors = [],
    considerRegionElements = [],
    customConsiderRegions = []
  }) {
    let response = null;
    let error;
    log.debug(`[${name}] : Preparing to capture screenshots on automate ...`);
    try {
      log.debug(`[${name}] : Marking automate session as percy ...`);
      const result = await this.percyScreenshotBegin(name);
      this.automateResults = JSON.parse(result.value);
      log.debug(`[${name}] : Fetching the debug url ...`);
      this.setDebugUrl();
      response = await super.screenshot(name, {
        ignoreRegionXpaths,
        ignoreRegionSelectors,
        ignoreRegionElements,
        customIgnoreRegions,
        considerRegionXpaths,
        considerRegionSelectors,
        considerRegionElements,
        customConsiderRegions
      });
    } catch (e) {
      error = e;
      throw e;
    } finally {
      await this.percyScreenshotEnd(name, error);
    }
    return response;
  }
  async getTiles(fullscreen) {
    var _this$options;
    if (!this.driver) throw new Error('Driver is null, please initialize driver with createDriver().');
    log.debug('Starting actual screenshotting phase');
    const dpr = await this.metaData.devicePixelRatio();
    const screenshotType = (_this$options = this.options) !== null && _this$options !== void 0 && _this$options.fullPage ? 'fullpage' : 'singlepage';
    const response = await TimeIt.run('percyScreenshot:screenshot', async () => {
      return await this.browserstackExecutor('percyScreenshot', {
        state: 'screenshot',
        percyBuildId: this.buildInfo.id,
        screenshotType: screenshotType,
        scaleFactor: dpr,
        options: this.options
      });
    });
    const responseValue = JSON.parse(response.value);
    if (!responseValue.success) {
      throw new Error('Failed to get screenshots from Automate.' + ' Check dashboard for error.');
    }
    const tiles = [];
    const tileResponse = JSON.parse(responseValue.result);
    log.debug('Tiles captured successfully');
    for (let tileData of tileResponse.tiles) {
      tiles.push(new Tile({
        statusBarHeight: tileData.status_bar || 0,
        navBarHeight: tileData.nav_bar || 0,
        headerHeight: tileData.header_height || 0,
        footerHeight: tileData.footer_height || 0,
        fullscreen,
        sha: tileData.sha.split('-')[0] // drop build id
      }));
    }
    const metadata = {
      screenshotType: screenshotType
    };
    return {
      tiles: tiles,
      domInfoSha: tileResponse.dom_sha,
      metadata: metadata
    };
  }
  async setDebugUrl() {
    if (!this.driver) throw new Error('Driver is null, please initialize driver with createDriver().');
    this.debugUrl = await Cache.withCache(Cache.bstackSessionDetails, this.driver.sessionId, async () => {
      return `https://automate.browserstack.com/builds/${this.automateResults.buildHash}/sessions/${this.automateResults.sessionHash}`;
    });
  }
  async getTag() {
    var _this$metaData$orient;
    if (!this.driver) throw new Error('Driver is null, please initialize driver with createDriver().');
    let {
      width,
      height
    } = await this.metaData.windowSize();
    const resolution = await this.metaData.screenResolution();
    const orientation = (_this$metaData$orient = this.metaData.orientation()) === null || _this$metaData$orient === void 0 ? void 0 : _this$metaData$orient.toLowerCase();
    const device = this.metaData.device();
    const tagData = {
      width,
      height,
      resolution,
      orientation,
      device
    };
    return await super.getTag(tagData);
  }
}