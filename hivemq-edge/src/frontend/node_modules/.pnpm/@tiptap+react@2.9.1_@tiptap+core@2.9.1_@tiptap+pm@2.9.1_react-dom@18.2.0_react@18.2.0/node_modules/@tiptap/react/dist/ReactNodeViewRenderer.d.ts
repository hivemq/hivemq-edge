import { Editor, NodeView, NodeViewProps, NodeViewRenderer, NodeViewRendererOptions } from '@tiptap/core';
import { Node, Node as ProseMirrorNode } from '@tiptap/pm/model';
import { Decoration, DecorationSource } from '@tiptap/pm/view';
import { ComponentType } from 'react';
import { ReactRenderer } from './ReactRenderer.js';
export interface ReactNodeViewRendererOptions extends NodeViewRendererOptions {
    /**
     * This function is called when the node view is updated.
     * It allows you to compare the old node with the new node and decide if the component should update.
     */
    update: ((props: {
        oldNode: ProseMirrorNode;
        oldDecorations: readonly Decoration[];
        oldInnerDecorations: DecorationSource;
        newNode: ProseMirrorNode;
        newDecorations: readonly Decoration[];
        innerDecorations: DecorationSource;
        updateProps: () => void;
    }) => boolean) | null;
    /**
     * The tag name of the element wrapping the React component.
     */
    as?: string;
    /**
     * The class name of the element wrapping the React component.
     */
    className?: string;
    /**
     * Attributes that should be applied to the element wrapping the React component.
     * If this is a function, it will be called each time the node view is updated.
     * If this is an object, it will be applied once when the node view is mounted.
     */
    attrs?: Record<string, string> | ((props: {
        node: ProseMirrorNode;
        HTMLAttributes: Record<string, any>;
    }) => Record<string, string>);
}
export declare class ReactNodeView<Component extends ComponentType<NodeViewProps> = ComponentType<NodeViewProps>, NodeEditor extends Editor = Editor, Options extends ReactNodeViewRendererOptions = ReactNodeViewRendererOptions> extends NodeView<Component, NodeEditor, Options> {
    /**
     * The renderer instance.
     */
    renderer: ReactRenderer<unknown, NodeViewProps>;
    /**
     * The element that holds the rich-text content of the node.
     */
    contentDOMElement: HTMLElement | null;
    /**
     * Setup the React component.
     * Called on initialization.
     */
    mount(): void;
    /**
     * Return the DOM element.
     * This is the element that will be used to display the node view.
     */
    get dom(): HTMLElement;
    /**
     * Return the content DOM element.
     * This is the element that will be used to display the rich-text content of the node.
     */
    get contentDOM(): HTMLElement | null;
    /**
     * On editor selection update, check if the node is selected.
     * If it is, call `selectNode`, otherwise call `deselectNode`.
     */
    handleSelectionUpdate(): void;
    /**
     * On update, update the React component.
     * To prevent unnecessary updates, the `update` option can be used.
     */
    update(node: Node, decorations: readonly Decoration[], innerDecorations: DecorationSource): boolean;
    /**
     * Select the node.
     * Add the `selected` prop and the `ProseMirror-selectednode` class.
     */
    selectNode(): void;
    /**
     * Deselect the node.
     * Remove the `selected` prop and the `ProseMirror-selectednode` class.
     */
    deselectNode(): void;
    /**
     * Destroy the React component instance.
     */
    destroy(): void;
    /**
     * Update the attributes of the top-level element that holds the React component.
     * Applying the attributes defined in the `attrs` option.
     */
    updateElementAttributes(): void;
}
/**
 * Create a React node view renderer.
 */
export declare function ReactNodeViewRenderer(component: ComponentType<NodeViewProps>, options?: Partial<ReactNodeViewRendererOptions>): NodeViewRenderer;
//# sourceMappingURL=ReactNodeViewRenderer.d.ts.map