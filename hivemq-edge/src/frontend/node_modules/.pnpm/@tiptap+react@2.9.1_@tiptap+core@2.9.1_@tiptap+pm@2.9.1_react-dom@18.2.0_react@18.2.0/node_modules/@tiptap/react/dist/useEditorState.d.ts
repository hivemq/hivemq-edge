import type { Editor } from '@tiptap/core';
export type EditorStateSnapshot<TEditor extends Editor | null = Editor | null> = {
    editor: TEditor;
    transactionNumber: number;
};
export type UseEditorStateOptions<TSelectorResult, TEditor extends Editor | null = Editor | null> = {
    /**
     * The editor instance.
     */
    editor: TEditor;
    /**
     * A selector function to determine the value to compare for re-rendering.
     */
    selector: (context: EditorStateSnapshot<TEditor>) => TSelectorResult;
    /**
     * A custom equality function to determine if the editor should re-render.
     * @default `deepEqual` from `fast-deep-equal`
     */
    equalityFn?: (a: TSelectorResult, b: TSelectorResult | null) => boolean;
};
/**
 * This hook allows you to watch for changes on the editor instance.
 * It will allow you to select a part of the editor state and re-render the component when it changes.
 * @example
 * ```tsx
 * const editor = useEditor({...options})
 * const { currentSelection } = useEditorState({
 *  editor,
 *  selector: snapshot => ({ currentSelection: snapshot.editor.state.selection }),
 * })
 */
export declare function useEditorState<TSelectorResult>(options: UseEditorStateOptions<TSelectorResult, Editor>): TSelectorResult;
/**
 * This hook allows you to watch for changes on the editor instance.
 * It will allow you to select a part of the editor state and re-render the component when it changes.
 * @example
 * ```tsx
 * const editor = useEditor({...options})
 * const { currentSelection } = useEditorState({
 *  editor,
 *  selector: snapshot => ({ currentSelection: snapshot.editor.state.selection }),
 * })
 */
export declare function useEditorState<TSelectorResult>(options: UseEditorStateOptions<TSelectorResult, Editor | null>): TSelectorResult | null;
//# sourceMappingURL=useEditorState.d.ts.map