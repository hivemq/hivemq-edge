# EtherNet/IP

The protocol stack

- ODVA is the standards organization (Open DeviceNet Vendors Association) that owns and publishes CIP.
- CIP (Common Industrial Protocol) is the application layer: an object model (everything is a Class / Instance / Attribute) plus a set of services (Get_Attribute_Single, Read Tag, Write Tag, …).
- EtherNet/IP = "EtherNet/Industrial Protocol" = CIP encapsulated over standard Ethernet/TCP/IP (TCP 44818 for explicit messaging, UDP for implicit I/O).

## old ethernet/ip protocola adapter vs new odva one


| Descr           | etherip (old)                                                         | etherip-cip-odva (new)                             |
|-----------------|-----------------------------------------------------------------------|----------------------------------------------------|
| CIP service     | Read Tag (Rockwell symbolic service)                                  | Get_Attribute_Single (generic CIP service)         |
| Addressing      | Symbolic — tag name (MyTag.Member)                                    | Logical — @class/instance/attribute (@4/100/1)     |
| Code            | client.readTags("tagName")                                            | client.getAttributeSingle(logicalAddressPath, …)   |
| Who supports it | Rockwell/Allen-Bradley ControlLogix & CompactLogix (vendor extension) | Any CIP-conformant device (the ODVA-standard way)  |

The key point: symbolic Read-Tag is the Rockwell/Allen-Bradley flavor of CIP — it's a vendor extension that relies on the PLC having a tag database you can address by name. 
ODVA is the generic, standards-compliant path where data lives in the object model. You fetch it by numeric class/instance/attribute.

**We should probably support both schemes!**

