import utils from '@percy/sdk-utils';
import Cache from './util/cache.js';
const {
  request
} = utils;
const log = utils.logger('webdriver-utils:driver');
export default class Driver {
  constructor(sessionId, executorUrl, passedCapabilities) {
    this.sessionId = sessionId;
    this.executorUrl = executorUrl.includes('@') ? `https://${executorUrl.split('@')[1]}` : executorUrl;
    this.passedCapabilities = passedCapabilities;
  }
  static requestPostOptions(command) {
    return {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      },
      body: JSON.stringify(command)
    };
  }
  async getCapabilites() {
    return await Cache.withCache(Cache.caps, this.sessionId, async () => {
      try {
        const baseUrl = `${this.executorUrl}/session/${this.sessionId}`;
        const caps = JSON.parse((await request(baseUrl)).body);
        return caps.value;
      } catch (err) {
        log.warn(`Falling back to legacy protocol, Error: ${err.message}`);
        return this.passedCapabilities;
      }
    });
  }
  async getWindowSize() {
    const baseUrl = `${this.executorUrl}/session/${this.sessionId}/window/current/size`;
    const windowSize = JSON.parse((await request(baseUrl)).body);
    return windowSize;
  }

  // command => {script: "", args: []}
  async executeScript(command) {
    if (!command.constructor === Object || !(Object.keys(command).length === 2 && Object.keys(command).includes('script') && Object.keys(command).includes('args'))) {
      throw new Error('Please pass command as {script: "", args: []}');
    }
    // browser_executor is custom BS executor script, if there is anything extra it breaks
    // percy_automate_script is an anchor comment to identify percy automate scripts
    if (!command.script.includes('browserstack_executor')) {
      command.script = `/* percy_automate_script */ \n ${command.script}`;
    }
    const options = Driver.requestPostOptions(command);
    const baseUrl = `${this.executorUrl}/session/${this.sessionId}/execute/sync`;
    const response = JSON.parse((await request(baseUrl, options)).body);
    return response;
  }
  async takeScreenshot() {
    const baseUrl = `${this.executorUrl}/session/${this.sessionId}/screenshot`;
    const screenShot = JSON.parse((await request(baseUrl)).body);
    return screenShot.value;
  }
  async rect(elementId) {
    const baseUrl = `${this.executorUrl}/session/${this.sessionId}/element/${elementId}/rect`;
    const response = JSON.parse((await request(baseUrl)).body);
    return response.value;
  }
  async findElement(using, value) {
    const options = Driver.requestPostOptions({
      using,
      value
    });
    const baseUrl = `${this.executorUrl}/session/${this.sessionId}/element`;
    const response = JSON.parse((await request(baseUrl, options)).body);
    return response.value;
  }
  async findElementBoundingBox(using, value) {
    if (using === 'xpath') {
      return await this.findElementXpath(value);
    } else if (using === 'css selector') {
      return await this.findElementSelector(value);
    }
  }
  async findElementXpath(xpath) {
    xpath = xpath.replace(/'/g, '"');
    const command = {
      script: `return document.evaluate('${xpath}', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.getBoundingClientRect();`,
      args: []
    };
    const response = await this.executeScript(command);
    return response.value;
  }
  async findElementSelector(selector) {
    selector = selector.replace('\\', '\\\\');
    const command = {
      script: `return document.querySelector('${selector}').getBoundingClientRect();`,
      args: []
    };
    const response = await this.executeScript(command);
    return response.value;
  }
}