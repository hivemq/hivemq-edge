# MTConnect Protocol Adapter

## Design

### Schema Validation

The [Schema](https://github.com/mtconnect/schema) validation is XSD based. That implies a considerable performance overhead if the validation is performed per message. In general, there are 2 options:

1. Completely turn off the schema validation.
2. Implement the validation in Java to improve the performance.

(1) gets rid of the performance overhead, but allows malformed data to be sent to the broker. (2) performs the validation with a better performance, but still slows down the processing of the messages a little bit.

## Test

### Test Bed

There is a [Smart Manufacturing Systems (SMS) Test Bed](https://www.nist.gov/laboratories/tools-instruments/smart-manufacturing-systems-sms-test-bed) offering Volatile Data Stream (VDS).

- [VDS Schema](https://smstestbed.nist.gov/vds)
- [Real-time stream of most current value for each data item](https://smstestbed.nist.gov/vds/current)
- [Time series of most recent values collected for each data item](https://smstestbed.nist.gov/vds/sample)
- [Report of all data items available](https://smstestbed.nist.gov/vds/probe)

## References

- [mtconnect.org](https://www.mtconnect.org/)
- [Github](http://www.github.com/mtconnect)
- [Schema](https://github.com/mtconnect/schema)
