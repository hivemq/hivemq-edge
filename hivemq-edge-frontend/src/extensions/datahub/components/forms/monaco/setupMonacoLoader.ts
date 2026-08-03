import { loader } from '@monaco-editor/react'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/language/json/json.worker?worker'
import cssWorker from 'monaco-editor/language/css/css.worker?worker'
import htmlWorker from 'monaco-editor/language/html/html.worker?worker'
import tsWorker from 'monaco-editor/language/typescript/ts.worker?worker'

/**
 * `@monaco-editor/react` resolves Monaco from jsdelivr at runtime unless it is told otherwise, which
 * makes the editor — and every test that renders it — depend on the network, and serves whichever
 * version the CDN defaults to rather than the one pinned in package.json. Point the loader at the
 * installed package and let Vite bundle the web workers, so nothing is fetched from a CDN at build
 * or run time.
 *
 * Import this module for its side effect before any `<Editor>` is rendered.
 */
self.MonacoEnvironment = {
  getWorker(_workerId: string, label: string) {
    if (label === 'json') return new jsonWorker()
    if (label === 'css' || label === 'scss' || label === 'less') return new cssWorker()
    if (label === 'html' || label === 'handlebars' || label === 'razor') return new htmlWorker()
    if (label === 'typescript' || label === 'javascript') return new tsWorker()
    return new editorWorker()
  },
}

loader.config({ monaco })

// The CDN build published Monaco on `window` as a side effect of its AMD loader. Nothing in the app
// relies on that, but it is the handle the tests (and browser debugging) use to reach the editor
// instances, so keep it available now that the instance is supplied directly.
;(window as Window & { monaco?: typeof monaco }).monaco = monaco
