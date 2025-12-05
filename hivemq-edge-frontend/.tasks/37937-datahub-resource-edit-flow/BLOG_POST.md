## DataHub Resource Management: Create and Edit Schemas and Scripts Independently

### What It Is

HiveMQ Edge DataHub now lets you **create and edit schemas and transformation scripts directly from the main DataHub page**, independent of policy design. Instead of managing resources within the policy designer's node panels, you now work with dedicated resource tables and editors.

The feature provides two main resource types, each with dedicated management:

- **Schemas** - JSON and Protobuf schema definitions for data validation, created and edited from the Schemas tab with Monaco code editor support
- **Scripts** - JavaScript transformation functions for data processing, managed from the Scripts tab with full syntax highlighting
- **Simplified Node Panels** - When designing policies, simply select existing resources by name and version from dropdown menus
- **Version Management** - New resource versions are created automatically when editing, preserving resource history
- **Resource Tables** - View all your schemas and scripts at a glance with Name, Type, Version columns and one-click editing

### How It Works

1. **Navigate to the Schemas or Scripts tab** on the DataHub main page
2. **Click "Create New"** to open a dedicated resource editor in a side drawer
3. **Fill in resource details** - Enter name, select type (JSON/Protobuf for schemas), and write your definition or code in the Monaco editor
4. **Save your resource** - It's immediately available for use in any policy
5. **Use in policies** - When designing a policy, double-click a schema or function node and select your resource from a simple dropdown

All resource operations complete instantly. The Monaco editor provides professional code editing with syntax highlighting and validation. When you edit an existing resource, a new version is created automatically—your previous versions remain intact.

![DataHub Resource Tables showing Schemas tab with Create New button and resource list](./screenshot-schema-table-create-button.png)

### How It Helps

#### Clearer Mental Model

Resources are now first-class entities managed independently before use. You create your schemas and scripts as standalone assets, then reference them in policies—mirroring how you naturally think about your data architecture.

#### Faster Resource Organization

Create and organize all your schemas and scripts before building policies. No need to context-switch between "I'm designing a policy" and "I need to write this schema definition." Each task has its dedicated workspace.

#### Simpler Policy Configuration

Policy node panels are now straightforward—just select which resource to use from a dropdown. No complex forms, no conditional logic for "create new vs select existing." Select name, select version, done.

#### Consistent Experience

Schemas and scripts follow identical patterns: same table layout, same editor interface, same versioning behavior. Learn it once for schemas, and you already know how to manage scripts.

### Looking Ahead

This represents the **initial separation** of resource management from policy configuration. **We're collecting feedback on the resource editor workflows and simplified node panels** to ensure they match your real-world usage patterns.

Consider this the **foundation for future resource management features** like bulk operations, resource templates, and enhanced version comparison tools. Your feedback on how you use schemas and scripts in production will directly shape these improvements.

If you discover workflows that could be smoother or have ideas for resource management enhancements, please share your insights!

---

**Try creating a schema from the Schemas tab in your next DataHub project—experience the clearer separation between resource management and policy design.**
