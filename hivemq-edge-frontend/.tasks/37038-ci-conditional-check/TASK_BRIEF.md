# Task: 37038-ci-conditional-check

## Objective

Implement the Group wizard for the Workspace Operation Wizard system, extending the work from task 38111 to support creating groups through the wizard interface.

## Context

The Ci Check github action is not working anymore

The release PRs have been completed without the CI Check properly running

The cause seems to be with the detection of changes in the different “areas of concern”: backend, frontend, and OpenAPI

The sub flows don't run, Ci Check validates itself by default
