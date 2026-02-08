# Plan: Add Optional base-dn Field to LDAP Authentication

## User Story Summary

Add an optional `base-dn` field to LDAP authentication configuration to allow specifying the domain's base distinguished name. This enables more flexible DN specification where both the search root and service account can be specified relative to a common base DN.

## Key Requirements

### Acceptance Criteria
1. Add optional `base-dn` field (backward compatible)
2. **Legacy Mode (no base-dn specified or empty):**
   - `rdns` is treated as absolute DN (current behavior)
   - Service account `rdns` is relative to search root `rdns`
3. **New Mode (base-dn specified):**
   - Search root `rdns` is relative to `base-dn`
   - Service account `rdns` is **also relative to `base-dn`** (NOT to search root)
   - This allows service accounts in different OUs than users
4. Use DN builders (UnboundID SDK's DN/RDN classes), not string concatenation
5. Include user DN construction for template/search approaches

### Out of Scope
- Any other LDAP configuration changes

## Current Implementation Analysis

### Key Files
- **Configuration Entity:** `hivemq-edge/src/main/java/com/hivemq/configuration/entity/api/ldap/LdapAuthenticationEntity.java`
  - Has `rdns` field (line 80-81) currently used as base DN
- **Service Account Entity:** `hivemq-edge/src/main/java/com/hivemq/configuration/entity/api/ldap/LdapSimpleBindEntity.java`
  - Has `rdns` field (line 17-18) for service account DN
- **Connection Properties:** `hivemq-edge/src/main/java/com/hivemq/api/auth/provider/impl/ldap/LdapConnectionProperties.java`
  - Record with connection configuration
  - `fromEntity()` factory method (lines 174-202)
  - `createUserDnResolver()` method (lines 251-270)
- **LDAP Client:** `hivemq-edge/src/main/java/com/hivemq/api/auth/provider/impl/ldap/LdapClient.java`
  - `start()` method builds service account DN (lines 124-133)
  - `getRolesForUser()` uses rdns as search base (line 317)
- **DN Resolvers:**
  - `TemplateDnResolver.java` - Template-based DN construction
  - `SearchFilterDnResolver.java` - Search-based DN resolution

### Current DN Construction (Legacy Behavior)

In `LdapClient.java` lines 124-133:
```java
final var baseDn = new DN(connectionProperties.rdns());
final var bindDn = new DN(ImmutableList.<RDN>builder()
    .add(new DN(simpleBindEntity.rdns()).getRDNs())  // Service account RDNs
    .add(baseDn.getRDNs())                           // Base DN RDNs
    .build());
```

Service account DN = service account rdns + base rdns

### Documentation Gap Found
Sample config files show `<user-dn-template>` and `<base-dn>` but current implementation uses `rdns`. This feature will align implementation with documented behavior.

## Implementation Plan

### Phase 1: Add Configuration Field

**File: `LdapAuthenticationEntity.java`**
- Add optional `baseDn` field with `@XmlElement(name = "base-dn")` annotation after line 81
- Add getter method `getBaseDn()` after line 125
- Update `equals()` method (lines 152-168) to include `baseDn`
- Update `hashCode()` method (lines 171-185) to include `baseDn`

### Phase 2: Update Connection Properties Record

**File: `LdapConnectionProperties.java`**
- Add `@Nullable String baseDn` parameter to record (after line 74)
- Update JavaDoc for record parameters (line 55) to explain dual-mode behavior
- Modify `fromEntity()` method (lines 174-202) to pass `entity.getBaseDn()`
- Add validation in compact constructor (lines 209-234):
  - Validate `baseDn` is a valid DN if present (using UnboundID `new DN(baseDn)`)
  - Validate `rdns` is a valid DN
- Update `equals()` and `hashCode()` methods (lines 339-376) to include `baseDn`

**Modify `createUserDnResolver()` method (lines 251-270):**
```java
// Compute effective base DN for user DN resolution
final String effectiveBaseDn;
if (baseDn == null || baseDn.isBlank()) {
    // Legacy mode: rdns is absolute base
    effectiveBaseDn = rdns;
} else {
    // New mode: combine rdns + baseDn
    effectiveBaseDn = new DN(ImmutableList.<RDN>builder()
        .add(new DN(rdns).getRDNs())
        .add(new DN(baseDn).getRDNs())
        .build()).toString();
}
// Pass effectiveBaseDn to resolvers...
```

### Phase 3: Update LDAP Client DN Construction

**File: `LdapClient.java`**

**Add private field to store effective search base:**
```java
private volatile DN effectiveSearchBaseDn;
```

**Modify `start()` method (lines 124-133) with dual-mode logic:**
```java
final DN effectiveBaseDn;
final DN serviceAccountDn;

if (connectionProperties.baseDn() == null || connectionProperties.baseDn().isBlank()) {
    // LEGACY MODE: rdns is absolute, service account rdns relative to it
    effectiveBaseDn = new DN(connectionProperties.rdns());
    serviceAccountDn = new DN(ImmutableList.<RDN>builder()
        .add(new DN(simpleBindEntity.rdns()).getRDNs())
        .add(effectiveBaseDn.getRDNs())
        .build());
} else {
    // NEW MODE: both rdns and service account rdns relative to baseDn
    final DN baseDn = new DN(connectionProperties.baseDn());

    // Search root: rdns + baseDn
    effectiveBaseDn = new DN(ImmutableList.<RDN>builder()
        .add(new DN(connectionProperties.rdns()).getRDNs())
        .add(baseDn.getRDNs())
        .build());

    // Service account: service rdns + baseDn (NOT relative to search root!)
    serviceAccountDn = new DN(ImmutableList.<RDN>builder()
        .add(new DN(simpleBindEntity.rdns()).getRDNs())
        .add(baseDn.getRDNs())
        .build());
}

this.effectiveSearchBaseDn = effectiveBaseDn;
final var bindRequest = new SimpleBindRequest(serviceAccountDn, simpleBindEntity.userPassword());
```

**Update `getRolesForUser()` method (line 317):**
- Change from `connectionProperties.rdns()` to use stored `effectiveSearchBaseDn`

### Phase 4: Update XML Schema

**File: `hivemq-edge/src/main/resources/config.xsd`**
- Add optional `<xs:element name="base-dn" type="nonEmptyString" minOccurs="0">` after line 1153
- Add documentation explaining dual-mode behavior
- Update documentation for existing `rdns` element (lines 1149-1153)

### Phase 5: Update Sample Configurations

**Files to update:**
- `config-sample-ldap-api.xml`
- `config-sample-ldap-system-cas-api.xml`

**Add new example:**
- `config-sample-ldap-legacy-api.xml` - Shows legacy mode without base-dn

**Show examples of:**
1. New mode with service account in different OU than users
2. Legacy mode for backward compatibility
3. Add comments explaining behavior differences

### Phase 6: Testing

**Unit Tests:**

**New file: `LdapConnectionPropertiesBaseDnTest.java`**
Location: `hivemq-edge/src/test/java/com/hivemq/api/auth/provider/impl/ldap/`

Test cases:
- Legacy mode: verify rdns is absolute, service account relative to rdns
- New mode: verify both rdns and service account relative to base-dn
- Edge cases: empty base-dn, whitespace, null behaves as legacy
- DN validation errors

**Update existing: `LdapConnectionPropertiesTest.java`**
- Add `baseDn` parameter (null) to existing test cases
- Test equals/hashCode with new field

**Integration Tests:**

**New file: `LdapClientBaseDnIntegrationTest.java`**
Location: `hivemq-edge/src/test/java/com/hivemq/api/auth/provider/impl/ldap/`

Using existing `LldapContainer` infrastructure:
- Test legacy mode authentication against real LDAP
- Test new mode with service account in different OU
- Verify role queries use correct search base
- Test both template and search-based DN resolution

**Test Scenarios:**

1. **Legacy Mode:**
```xml
<rdns>ou=people,dc=example,dc=com</rdns>
<simple-bind>
  <rdns>cn=admin</rdns>
</simple-bind>
```
Expected: Service account DN = `cn=admin,ou=people,dc=example,dc=com`

2. **New Mode:**
```xml
<rdns>ou=people</rdns>
<base-dn>dc=example,dc=com</base-dn>
<simple-bind>
  <rdns>cn=admin,ou=system</rdns>
</simple-bind>
```
Expected:
- Service account DN = `cn=admin,ou=system,dc=example,dc=com`
- Search base = `ou=people,dc=example,dc=com`

### Phase 7: Documentation

**JavaDoc Updates:**
- `LdapAuthenticationEntity`: Add class-level example showing both modes
- `LdapConnectionProperties`: Update record JavaDoc explaining dual-mode
- `LdapClient.start()`: Add comments explaining mode detection

**Sample Config Comments:**
Add migration guide showing legacy vs new configuration format

## Critical Files to Modify

1. `hivemq-edge/src/main/java/com/hivemq/configuration/entity/api/ldap/LdapAuthenticationEntity.java`
2. `hivemq-edge/src/main/java/com/hivemq/api/auth/provider/impl/ldap/LdapConnectionProperties.java`
3. `hivemq-edge/src/main/java/com/hivemq/api/auth/provider/impl/ldap/LdapClient.java`
4. `hivemq-edge/src/main/resources/config.xsd`
5. `hivemq-edge/src/test/java/com/hivemq/api/auth/provider/impl/ldap/LdapConnectionPropertiesBaseDnTest.java` (new)
6. `hivemq-edge/src/test/java/com/hivemq/api/auth/provider/impl/ldap/LdapClientBaseDnIntegrationTest.java` (new)
7. Sample config files in `hivemq-edge/src/distribution/conf/examples/configuration/api/`

## Key Implementation Insight

**Critical behavior difference between modes:**
- **Legacy:** Service account DN is nested under search root
- **New:** Service account DN and search root are **siblings** under base-dn

This allows service accounts to be in `ou=system,dc=example,dc=com` while users are searched in `ou=people,dc=example,dc=com`.

## Verification Steps

1. **Build:** `./gradlew build` succeeds
2. **Unit Tests:** All existing tests pass, new tests pass
3. **Integration Tests:** Both modes work against LldapContainer
4. **Backward Compatibility:** Existing configs without base-dn continue to work
5. **Manual Test:** Deploy with both config modes and verify authentication

## Rollout Considerations

- This is a purely additive feature (no deprecation needed)
- Existing configurations continue to work unchanged
- Consider logging a DEBUG message indicating which mode is active when LDAP client starts
- Document the feature in release notes as improvement for multi-OU LDAP setups
