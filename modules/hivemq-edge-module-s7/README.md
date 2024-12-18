New:

BOOL(Boolean.class),

BYTE(Byte.class),

INT16(Short.class),
UINT16(Short.class),

INT32(Integer.class),
UINT32(Integer.class),

INT64(Long.class),

REAL(Float.class),

LREAL(Double.class),

TIME(Long.class)

STRING(String.class),

DATE(LocalDate.class),
TIME_OF_DAY(LocalTime.class),
DATE_AND_TIME(LocalDateTime.class),


NULL((short) 0x00, null),

BOOL((short) 0x01, Boolean.class),

BYTE((short) 0x02, Byte.class),
SINT((short) 0x21, Byte.class),

WORD((short) 0x03, Short.class),
USINT((short) 0x11, Short.class),
INT((short) 0x22, Short.class),
WCHAR((short) 0x42, Short.class),

DWORD((short) 0x04, Integer.class),
UINT((short) 0x12, Integer.class),
DINT((short) 0x23, Integer.class),

LWORD((short) 0x05, Long.class),
UDINT((short) 0x13, Long.class),
LINT((short) 0x24, Long.class),

ULINT((short) 0x14, BigInteger.class),

STRING((short) 0x43, String.class),
WSTRING((short) 0x44, String.class),

REAL((short) 0x31, Float.class),
LREAL((short) 0x32, Double.class),

CHAR((short) 0x41, Character.class),


TIME((short) 0x51, Duration.class),
LTIME((short) 0x52, Duration.class),
DATE((short) 0x53, LocalDate.class),
LDATE((short) 0x54, LocalDate.class),
TIME_OF_DAY((short) 0x55, LocalTime.class),
LTIME_OF_DAY((short) 0x56, LocalTime.class),
DATE_AND_TIME((short) 0x57, LocalDateTime.class),
LDATE_AND_TIME((short) 0x58, LocalDateTime.class),
RAW_BYTE_ARRAY((short) 0x71, Byte.class);
