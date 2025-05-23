(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('@tiptap/core'), require('@tiptap/pm/state'), require('@tiptap/pm/view')) :
  typeof define === 'function' && define.amd ? define(['exports', '@tiptap/core', '@tiptap/pm/state', '@tiptap/pm/view'], factory) :
  (global = typeof globalThis !== 'undefined' ? globalThis : global || self, factory(global["@tiptap/extension-placeholder"] = {}, global.core, global.state, global.view));
})(this, (function (exports, core, state, view) { 'use strict';

  /**
   * This extension allows you to add a placeholder to your editor.
   * A placeholder is a text that appears when the editor or a node is empty.
   * @see https://www.tiptap.dev/api/extensions/placeholder
   */
  const Placeholder = core.Extension.create({
      name: 'placeholder',
      addOptions() {
          return {
              emptyEditorClass: 'is-editor-empty',
              emptyNodeClass: 'is-empty',
              placeholder: 'Write something …',
              showOnlyWhenEditable: true,
              showOnlyCurrent: true,
              includeChildren: false,
          };
      },
      addProseMirrorPlugins() {
          return [
              new state.Plugin({
                  key: new state.PluginKey('placeholder'),
                  props: {
                      decorations: ({ doc, selection }) => {
                          const active = this.editor.isEditable || !this.options.showOnlyWhenEditable;
                          const { anchor } = selection;
                          const decorations = [];
                          if (!active) {
                              return null;
                          }
                          const isEmptyDoc = this.editor.isEmpty;
                          doc.descendants((node, pos) => {
                              const hasAnchor = anchor >= pos && anchor <= pos + node.nodeSize;
                              const isEmpty = !node.isLeaf && core.isNodeEmpty(node);
                              if ((hasAnchor || !this.options.showOnlyCurrent) && isEmpty) {
                                  const classes = [this.options.emptyNodeClass];
                                  if (isEmptyDoc) {
                                      classes.push(this.options.emptyEditorClass);
                                  }
                                  const decoration = view.Decoration.node(pos, pos + node.nodeSize, {
                                      class: classes.join(' '),
                                      'data-placeholder': typeof this.options.placeholder === 'function'
                                          ? this.options.placeholder({
                                              editor: this.editor,
                                              node,
                                              pos,
                                              hasAnchor,
                                          })
                                          : this.options.placeholder,
                                  });
                                  decorations.push(decoration);
                              }
                              return this.options.includeChildren;
                          });
                          return view.DecorationSet.create(doc, decorations);
                      },
                  },
              }),
          ];
      },
  });

  exports.Placeholder = Placeholder;
  exports.default = Placeholder;

  Object.defineProperty(exports, '__esModule', { value: true });

}));
//# sourceMappingURL=index.umd.js.map
