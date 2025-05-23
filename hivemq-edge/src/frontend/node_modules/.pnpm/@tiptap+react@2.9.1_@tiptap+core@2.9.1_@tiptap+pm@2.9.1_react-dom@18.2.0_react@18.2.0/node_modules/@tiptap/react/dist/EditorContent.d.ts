import { Editor } from '@tiptap/core';
import React, { ForwardedRef, HTMLProps } from 'react';
export interface EditorContentProps extends HTMLProps<HTMLDivElement> {
    editor: Editor | null;
    innerRef?: ForwardedRef<HTMLDivElement | null>;
}
export declare class PureEditorContent extends React.Component<EditorContentProps, {
    hasContentComponentInitialized: boolean;
}> {
    editorContentRef: React.RefObject<any>;
    initialized: boolean;
    unsubscribeToContentComponent?: () => void;
    constructor(props: EditorContentProps);
    componentDidMount(): void;
    componentDidUpdate(): void;
    init(): void;
    componentWillUnmount(): void;
    render(): React.JSX.Element;
}
export declare const EditorContent: React.MemoExoticComponent<React.ForwardRefExoticComponent<Omit<EditorContentProps, "ref"> & React.RefAttributes<HTMLDivElement>>>;
//# sourceMappingURL=EditorContent.d.ts.map