# @percy/env

This package provides various CI/CD support for Percy by coalescing different environment variables
into a common interface for consumption by `@percy/client`.

## Supported Environments

- [AppVeyor](https://www.browserstack.com/docs/percy/ci-cd/appveyor)
- [Azure Pipelines](https://www.browserstack.com/docs/percy/ci-cd/azure-pipelines)
- [Bitbucket Pipelines](https://www.browserstack.com/docs/percy/ci-cd/bitbucket-pipeline)
- [Buildkite](https://www.browserstack.com/docs/percy/ci-cd/buildkite)
- [CircleCI](https://www.browserstack.com/docs/percy/ci-cd/circleci)
- [Codeship](https://www.browserstack.com/docs/percy/ci-cd/codeship)
- [Drone CI](https://docs.percy.io/docs/drone)
- [GitHub Actions](https://www.browserstack.com/docs/percy/ci-cd/github-actions)
- [GitLab CI](https://www.browserstack.com/docs/percy/ci-cd/gitlab)
- [Heroku CI](#supported-environments) (needs doc)
- [Jenkins](https://www.browserstack.com/docs/percy/ci-cd/jenkins)
- [Jenkins PRB](https://www.browserstack.com/docs/percy/ci-cd/jenkins)
- [Netlify](https://www.browserstack.com/docs/percy/ci-cd/netlify)
- [Probo.CI](#supported-environments) (needs doc)
- [Semaphore](https://www.browserstack.com/docs/percy/ci-cd/semaphore)
- [Travis CI](https://www.browserstack.com/docs/percy/ci-cd/travis-ci)

## Percy Environment Variables

The following variables may be defined to override the respective derived CI environment variables.

```bash
PERCY_COMMIT          # build commit sha
PERCY_BRANCH          # build branch name
PERCY_PULL_REQUEST    # associated PR number
PERCY_PARALLEL_NONCE  # parallel nonce unique for this CI workflow
PERCY_PARALLEL_TOTAL  # total number of parallel shards
```

Additional Percy specific environment variable may be set to control aspects of your Percy build.

```bash
PERCY_TARGET_COMMIT   # percy target commit sha
PERCY_TARGET_BRANCH   # percy target branch name
PERCY_PARTIAL_BUILD   # if this build was marked as partial
```

## Adding Environment Support

1. Add CI detection to [`environment.js`](./src/environment.js)
2. Add respective environment variables
3. Add a dedicated CI test suite
4. Open a Pull Request!
