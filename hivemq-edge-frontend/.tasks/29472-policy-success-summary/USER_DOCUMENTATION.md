## Policy Success Summary: Understand Your Impact Before Publishing

### What It Is

When you validate a Data Hub policy, you now receive a comprehensive success report instead of just a confirmation message. This report shows you exactly what will be created or modified before you click publish, giving you confidence in your changes.

The success summary includes three key sections:

- **Policy Overview** - Quick snapshot of your policy with its ID, type (Data or Behavior), and key configuration details
- **Resources Breakdown** - Complete list of all schemas, scripts, and transformations that will be created or modified, with status indicators for each
- **JSON Payload View** (optional) - Collapsible, syntax-highlighted display of the complete policy configuration in JSON format with separate tabs for policies, schemas, and scripts

### How It Works

1. **Design your policy** in the Data Hub designer using the visual interface
2. **Click Validate** to check if your policy is correct
3. **Review the success summary** that appears showing what will be published
4. **Examine resources** in the breakdown to see all schemas and scripts involved
5. **Check JSON details** (optional) by expanding the JSON view if you want to see the raw configuration
6. **Click Publish** with confidence knowing exactly what changes will be made

The summary displays automatically when validation succeeds. All resources are clearly labeled as either "New" or "Update" so you understand which items are being created versus modified.

![Policy Success Summary - Showing overview, resources, and JSON payload details](./screenshot-policy-success-summary.png)

### How It Helps

#### See Impact Before Publishing

No more surprises after publishing. Review all resources being created or modified upfront, including any schemas or scripts required by your policy.

#### Understand Resource Dependencies

The breakdown clearly shows which schemas and scripts are needed for your policy to work, helping you understand the complete picture of what's being deployed.

#### Verify Your Configuration

The optional JSON view lets you inspect the raw policy definition if you need to verify specific configuration details. Syntax highlighting and organized tabs make it easy to review complex policies without getting lost in raw data.

#### Trust Your Changes

Whether you're creating a new policy or updating an existing one, the status badges and comprehensive summary give you the confidence to publish without uncertainty.

### Looking Ahead

The policy success summary is designed to evolve based on your feedback. As policies become more complex and topologies more diverse, we may add additional insights such as performance impact estimates or compatibility warnings. The breakdown and JSON view provide a foundation that will grow with your needs.

If you have suggestions on additional information that would help you validate policies more effectively, please share your feedback!

---

**Review your policy's impact in the success summary, then publish with confidence.**
