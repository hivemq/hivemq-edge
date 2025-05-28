// Extending `Map` to allow us to link Key and Values together

const ledger = new Map();
function registerUsage({
  typeKey,
  mount
}) {
  const entry = ledger.get(typeKey);
  if (entry) {
    entry.usageCount++;
    return entry;
  }
  const initial = {
    typeKey,
    unmount: mount(),
    usageCount: 1
  };
  ledger.set(typeKey, initial);
  return initial;
}
export function register(args) {
  const entry = registerUsage(args);
  return function unregister() {
    entry.usageCount--;
    if (entry.usageCount > 0) {
      return;
    }
    // Only a single usage left, remove it
    entry.unmount();
    ledger.delete(args.typeKey);
  };
}