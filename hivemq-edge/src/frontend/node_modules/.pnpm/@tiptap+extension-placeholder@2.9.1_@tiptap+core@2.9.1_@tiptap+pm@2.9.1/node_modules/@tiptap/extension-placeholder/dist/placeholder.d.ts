import { Editor, Extension } from '@tiptap/core';
import { Node as ProsemirrorNode } from '@tiptap/pm/model';
export interface PlaceholderOptions {
    /**
     * **The class name for the empty editor**
     * @default 'is-editor-empty'
     */
    emptyEditorClass: string;
    /**
     * **The class name for empty nodes**
     * @default 'is-empty'
     */
    emptyNodeClass: string;
    /**
     * **The placeholder content**
     *
     * You can use a function to return a dynamic placeholder or a string.
     * @default 'Write something …'
     */
    placeholder: ((PlaceholderProps: {
        editor: Editor;
        node: ProsemirrorNode;
        pos: number;
        hasAnchor: boolean;
    }) => string) | string;
    /**
     * See https://github.com/ueberdosis/tiptap/pull/5278 for more information.
     * @deprecated This option is no longer respected and this type will be removed in the next major version.
     */
    considerAnyAsEmpty?: boolean;
    /**
     * **Checks if the placeholder should be only shown when the editor is editable.**
     *
     * If true, the placeholder will only be shown when the editor is editable.
     * If false, the placeholder will always be shown.
     * @default true
     */
    showOnlyWhenEditable: boolean;
    /**
     * **Checks if the placeholder should be only shown when the current node is empty.**
     *
     * If true, the placeholder will only be shown when the current node is empty.
     * If false, the placeholder will be shown when any node is empty.
     * @default true
     */
    showOnlyCurrent: boolean;
    /**
     * **Controls if the placeholder should be shown for all descendents.**
     *
     * If true, the placeholder will be shown for all descendents.
     * If false, the placeholder will only be shown for the current node.
     * @default false
     */
    includeChildren: boolean;
}
/**
 * This extension allows you to add a placeholder to your editor.
 * A placeholder is a text that appears when the editor or a node is empty.
 * @see https://www.tiptap.dev/api/extensions/placeholder
 */
export declare const Placeholder: Extension<PlaceholderOptions, any>;
//# sourceMappingURL=placeholder.d.ts.map