# UBL Status Code Mapping - JDE UDC Table Setup

## Overview

Status codes in the UBL lifecycle tables are mapped from descriptive codes to JDE-compliant UDC codes to fit the 10-character limit in field `F564235.USK74RSCD`.

## JDE UDC Table Configuration

### Table: 74/ST (UBL Status Codes)

**System Code:** 74  
**User Defined Code:** ST  
**Description:** UBL Invoice Lifecycle Status Codes

### UDC Entries

Create the following entries in JDE UDC table 74/ST:

| Code | Description | Hard Coded | Special Handling |
|------|-------------|------------|------------------|
| CREATED | Invoice created in JDE | N | N |
| VALIDATED | Validation successful | N | N |
| VALID_WARN | Validation with warnings | N | N |
| SENT | Sent to PA | N | N |
| DEPOSEE | Deposited on PA | N | N |
| REJETEE | Rejected by PA | N | N |
| MISE_DISPO | Available for customer | N | N |
| REFUSEE | Refused by customer | N | N |
| ENCAISSEE | Payment confirmed | N | N |
| ERROR | Technical error | N | N |
| ERR_VALID | Validation error | N | N |
| ERR_PA | PA communication error | N | N |
| PENDING | Pending processing | N | N |
| CANCELLED | Cancelled | N | N |

### SQL to Insert UDC Values

```sql
-- Insert UDC codes for table 74/ST
INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'CREATED', 'Invoice created in JDE', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'VALIDATED', 'Validation successful', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'VALID_WARN', 'Validation with warnings', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'SENT', 'Sent to PA', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'DEPOSEE', 'Deposited on PA', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'REJETEE', 'Rejected by PA', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'MISE_DISPO', 'Available for customer', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'REFUSEE', 'Refused by customer', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'ENCAISSEE', 'Payment confirmed', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'ERROR', 'Technical error', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'ERR_VALID', 'Validation error', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'ERR_PA', 'PA communication error', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'PENDING', 'Pending processing', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
VALUES ('74', 'ST', 'CANCELLED', 'Cancelled', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);

COMMIT;
```

## Status Code Mapping File

**Location:** `src/custom/resources/ubl_status_codes.properties`

This properties file maps descriptive status codes used in the application to JDE UDC codes:

```properties
# Maps application codes to JDE UDC codes (74/ST)
CREATED=CREATED
VALIDATED=VALIDATED
VALIDATED_WARN=VALID_WARN
SENT=SENT
DEPOSEE=DEPOSEE
REJETEE=REJETEE
MISE_A_DISPOSITION=MISE_DISPO
REFUSEE=REFUSEE
ENCAISSEE=ENCAISSEE
ERROR=ERROR
ERROR_VALIDATION=ERR_VALID
ERROR_PA=ERR_PA
PENDING=PENDING
CANCELLED=CANCELLED
```

## Implementation Details

### Automatic Mapping

The `UBLDatabaseHandler` class automatically:
1. Loads the mapping file at initialization
2. Maps descriptive codes to JDE codes when inserting lifecycle events
3. Truncates codes exceeding 10 characters if needed
4. Logs warnings for unmapped or truncated codes

### Usage in Code

```java
// Application uses descriptive codes
dbHandler.insertLifecycleEvent(docID, dct, kco, "VALIDATED_WARN", "Validation with warnings");

// Automatically mapped to JDE code
// Database receives: "VALID_WARN" (10 chars)
```

### Console Output

```
 ** INFO ** DB ** Status code mapping loaded: 14 codes
 ** SUCCESS ** DB ** F564235 : Lifecycle event 'VALID_WARN' inserted for document 50816470
```

## Verification Queries

```sql
-- Check UDC codes are configured
SELECT DRSY, DRRT, DRKY, DRDL01, DRDL02
FROM PRODDTA.F0005
WHERE DRSY = '74' AND DRRT = 'ST'
ORDER BY DRKY;

-- Check lifecycle events with descriptions
SELECT l.USDOC, l.USK74RSCD, u.DRDL01 as STATUS_DESC, l.USK74MSG1, l.USTRDJ
FROM PRODDTA.F564235 l
LEFT JOIN PRODDTA.F0005 u ON u.DRSY = '74' AND u.DRRT = 'ST' AND u.DRKY = l.USK74RSCD
WHERE l.USDOC = 50816470
ORDER BY l.USSEQN;

-- Count invoices by status with descriptions
SELECT l.USK74RSCD, u.DRDL01 as STATUS_DESC, COUNT(*) as COUNT
FROM PRODDTA.F564235 l
LEFT JOIN PRODDTA.F0005 u ON u.DRSY = '74' AND u.DRRT = 'ST' AND u.DRKY = l.USK74RSCD
GROUP BY l.USK74RSCD, u.DRDL01
ORDER BY COUNT(*) DESC;
```

## Adding New Status Codes

To add a new status code:

1. **Add to properties file** (`ubl_status_codes.properties`):
   ```properties
   NEW_STATUS=NEW_STAT
   ```

2. **Add to JDE UDC table 74/ST**:
   ```sql
   INSERT INTO PRODDTA.F0005 (DRSY, DRRT, DRKY, DRDL01, DRDL02, DTUSER, DTPID, DTUPMJ, DTUPMT)
   VALUES ('74', 'ST', 'NEW_STAT', 'New status description', '', 'NOMAUBL', 'NOMAUBL', 125353, 120000);
   ```

3. **Rebuild and deploy** the application

4. **No code changes required** - mapping is automatic

## Benefits

✅ **JDE Compliance:** All codes fit 10-character limit  
✅ **Descriptive Code:** Application uses readable codes  
✅ **UDC Integration:** Codes available in JDE forms/reports  
✅ **Easy Maintenance:** Add codes without code changes  
✅ **Automatic Truncation:** Prevents database errors  
✅ **Warning Logs:** Alerts for configuration issues  

## Notes

- Field `F564235.USK74RSCD` is VARCHAR2(10) - maximum 10 characters
- Field `F564231.UHK74INVST` is VARCHAR2(20) - can hold longer codes but mapping is still applied for consistency
- Properties file must be in classpath: `/custom/resources/ubl_status_codes.properties`
- Missing mapping: code used as-is (truncated if > 10 chars)
- Case-sensitive mapping keys
