import { type EditorOptions, Editor } from '@tiptap/core';
import { DependencyList } from 'react';
/**
 * The options for the `useEditor` hook.
 */
export type UseEditorOptions = Partial<EditorOptions> & {
    /**
     * Whether to render the editor on the first render.
     * If client-side rendering, set this to `true`.
     * If server-side rendering, set this to `false`.
     * @default true
     */
    immediatelyRender?: boolean;
    /**
     * Whether to re-render the editor on each transaction.
     * This is legacy behavior that will be removed in future versions.
     * @default true
     */
    shouldRerenderOnTransaction?: boolean;
};
/**
 * This hook allows you to create an editor instance.
 * @param options The editor options
 * @param deps The dependencies to watch for changes
 * @returns The editor instance
 * @example const editor = useEditor({ extensions: [...] })
 */
export declare function useEditor(options: UseEditorOptions & {
    immediatelyRender: true;
}, deps?: DependencyList): Editor;
/**
 * This hook allows you to create an editor instance.
 * @param options The editor options
 * @param deps The dependencies to watch for changes
 * @returns The editor instance
 * @example const editor = useEditor({ extensions: [...] })
 */
export declare function useEditor(options?: UseEditorOptions, deps?: DependencyList): Editor | null;
//# sourceMappingURL=useEditor.d.ts.map