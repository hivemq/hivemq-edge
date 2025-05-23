import { Editor } from '@tiptap/core';
import React, { HTMLAttributes, ReactNode } from 'react';
import { UseEditorOptions } from './useEditor.js';
export type EditorContextValue = {
    editor: Editor | null;
};
export declare const EditorContext: React.Context<EditorContextValue>;
export declare const EditorConsumer: React.Consumer<EditorContextValue>;
/**
 * A hook to get the current editor instance.
 */
export declare const useCurrentEditor: () => EditorContextValue;
export type EditorProviderProps = {
    children?: ReactNode;
    slotBefore?: ReactNode;
    slotAfter?: ReactNode;
    editorContainerProps?: HTMLAttributes<HTMLDivElement>;
} & UseEditorOptions;
/**
 * This is the provider component for the editor.
 * It allows the editor to be accessible across the entire component tree
 * with `useCurrentEditor`.
 */
export declare function EditorProvider({ children, slotAfter, slotBefore, editorContainerProps, ...editorOptions }: EditorProviderProps): React.JSX.Element | null;
//# sourceMappingURL=Context.d.ts.map