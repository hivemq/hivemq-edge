import utils from '@percy/sdk-utils';
const {
  request
} = utils;
export default class PlaywrightDriver {
  constructor(sessionId) {
    this.sessionId = sessionId;
  }
  requestPostOptions(command) {
    return {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      },
      body: JSON.stringify(command)
    };
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
    const options = this.requestPostOptions(command);
    const baseUrl = `https://cdp.browserstack.com/wd/hub/session/${this.sessionId}/execute`;
    const response = JSON.parse((await request(baseUrl, options)).body);
    return response;
  }
}