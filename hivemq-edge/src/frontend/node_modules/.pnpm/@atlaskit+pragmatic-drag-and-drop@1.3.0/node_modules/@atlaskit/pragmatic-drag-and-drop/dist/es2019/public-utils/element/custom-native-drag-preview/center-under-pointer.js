export const centerUnderPointer = function centerUnderPointer({
  container
}) {
  const rect = container.getBoundingClientRect();
  return {
    x: rect.width / 2,
    y: rect.height / 2
  };
};