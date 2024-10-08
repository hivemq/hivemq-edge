const treeView = {
  "[role='tree']": {
    '--spacing': '1.5rem',
    '--radius': '10px',
  },
  '.tree-branch-wrapper': {
    // display: 'flex',
    // flexDirection: 'row',
    // alignItems: 'center',
  },
  '.tree-branch-wrapper, .tree-leaf-list-item': {
    listStyleType: 'none',
  },
  // "[role='tree'] ul li": {
  //   borderLeft: '2px solid #ddd',
  // },
  // "[role='tree'] ul li:last-child": {
  //   borderColor: 'transparent',
  // },
  // "[role='tree'] ul li::before": {
  //   content: "''",
  //   display: 'block',
  //   position: 'absolute',
  //   top: 'calc(var(--spacing) / -2)',
  //   left: '-2px',
  //   width: 'calc(var(--spacing) + 2px)',
  //   height: 'calc(var(--spacing) + 2px)',
  //   border: 'solid #ddd',
  //   borderWidth: '0 0 2px 2px',
  // },
}

export default treeView
