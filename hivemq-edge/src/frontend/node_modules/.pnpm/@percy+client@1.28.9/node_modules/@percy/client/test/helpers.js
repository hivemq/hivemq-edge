import EventEmitter from 'events';
import url from 'url';

// Mock response class used for mock requests
export class MockResponse extends EventEmitter {
  constructor(options) {
    Object.assign(super(), options);
  }

  resume() {}
  setEncoding() {}
  pipe = stream => this
    .on('data', d => stream.write(d))
    .on('end', () => stream.end());
};

// Mock request class automates basic mocking necessities
export class MockRequest extends EventEmitter {
  constructor(reply, url, opts, cb) {
    // handle optional url string
    if (url && typeof url === 'string') {
      let { protocol, hostname, port, pathname, search, hash } = new URL(url);
      opts = { ...opts, protocol, hostname, port, path: pathname + search + hash };
    } else if (typeof url !== 'string') {
      opts = url;
    }

    Object.assign(super(), opts, { reply });
    if (cb) this.on('response', cb);
  }

  // useful for logs/tests
  get url() {
    return new URL(this.path, url.format(this)).href;
  }

  // kick off a reply response on request end
  end(body) {
    // process async but return sync
    (async () => {
      try { this.body = JSON.parse(body); } catch {}
      let [statusCode, data = '', headers = {}] = await this.reply?.(this) ?? [];

      if (data && typeof data !== 'string') {
        // handle common json data
        headers['content-type'] = headers['content-type'] || 'application/json';
        data = JSON.stringify(data);
      } else if (!statusCode) {
        // no status code was mocked
        data = `Not mocked ${this.url}`;
        statusCode = 404;
      }

      // automate content-length header
      if (data != null && !headers['content-length']) {
        headers['content-length'] = Buffer.byteLength(data);
      }

      // create and trigger a mock response
      let res = new MockResponse({ statusCode, headers });
      this.emit('response', res);

      // maybe delay response data
      setTimeout(() => {
        res.emit('data', Buffer.from(data));
        res.emit('end');
      }, this.delay);
    })();

    return this;
  }
}

// Mock request responses using jasmine spies
export async function mockRequests(baseUrl, defaultReply = () => [200]) {
  let { protocol, hostname, pathname } = new URL(baseUrl);
  let { default: http } = await import(protocol === 'https:' ? 'https' : 'http');

  if (!jasmine.isSpy(http.request)) {
    spyOn(http, 'request').and.callFake((...a) => new MockRequest(null, ...a));
    spyOn(http, 'get').and.callFake((...a) => new MockRequest(null, ...a).end());
    mockRequests.spies = new Map();
  }

  let any = jasmine.anything();
  let match = o => o.hostname === hostname && (o.path ?? o.pathname).startsWith(pathname);
  let reply = jasmine.createSpy('reply').and.callFake(defaultReply);
  let spies = mockRequests.spies.get(baseUrl) ?? {};

  spies.request = spies.request ?? http.request.withArgs({ asymmetricMatch: match });
  spies.request.and.callFake((...a) => new MockRequest(reply, ...a));
  spies.get = spies.get ?? http.get.withArgs({ asymmetricMatch: u => match(new URL(u)) }, any, any);
  spies.get.and.callFake((...a) => new MockRequest(reply, ...a).end());
  mockRequests.spies.set(baseUrl, spies);

  return reply;
}

// Group of helpers to mock Percy API requests
export const api = {
  DEFAULT_REPLIES: {
    '/builds': () => [201, {
      data: {
        id: '123',
        attributes: {
          'build-number': 1,
          'web-url': 'https://percy.io/test/test/123'
        }
      }
    }],

    '/builds/123/snapshots': ({ body }) => [201, {
      data: {
        id: '4567',
        attributes: body.attributes,
        relationships: {
          'missing-resources': {
            data: body.data.relationships.resources
              .data.map(({ id }) => ({ id }))
          }
        }
      }
    }],

    '/snapshots/4567/comparisons': ({ body }) => [201, {
      data: {
        id: '891011',
        attributes: body.attributes,
        relationships: body.relationships
      }
    }],
    '/job_status?sync=true&type=snapshot&id=4567': () => [201, {
      4567: {
        status: true,
        nextPoll: 8,
        error: null
      }
    }],
    '/snapshots/4567?sync=true&response_format=sync-cli': () => [201, {
      name: 'test snapshot',
      diff_ratio: 0
    }],
    '/discovery/device-details?build_id=123': () => [201, {
      data: []
    }],
    '/discovery/device-details': () => [201, {
      data: []
    }],
    '/logs': () => [200, 'random_sha']
  },

  async mock({ delay = 10 } = {}) {
    this.replies = {};
    this.requests = {};

    await mockRequests('https://percy.io/api/v1', req => {
      let path = req.path.replace('/api/v1', '');

      let reply = (this.replies[path] && (
        this.replies[path].length > 1
          ? this.replies[path].shift()
          : this.replies[path][0]
      )) || this.DEFAULT_REPLIES[path];

      this.requests[path] = this.requests[path] || [];
      this.requests[path].push(req);

      if (delay) req.delay = delay;
      return reply?.(req) ?? [200];
    });
  },

  reply(path, handler) {
    this.replies[path] = this.replies[path] || [];
    this.replies[path].push(handler);
    return this;
  }
};

export default api;
