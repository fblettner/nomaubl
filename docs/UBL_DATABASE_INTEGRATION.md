# UBL Database Integration Guide

## Overview

This document describes the Oracle database integration for UBL invoice data according to specification **STD010301 UBL Database Specification**.

The integration populates the following Oracle tables **just before sending to PA (Plateforme Agréée)**:
- **F564231** - UBL Header (EN 16931 invoice header)
- **F564233** - Invoice Lines (line items)
- **F564234** - VAT Summary (tax breakdown by rate)
- **F564235** - Lifecycle Events (status history)
- **F564236** - Validation Results (Schematron errors/warnings)

## Configuration


### 2. Database Connection

The system uses the existing database connection configured in the global resource:

```xml
<resource name="global">
    <property name="URL">jdbc:oracle:thin:@hostname:1521:SID</property>
    <property name="schema">PRODDTA</property>
    <property name="updateDB">Y</property>
    <property name="DBUser"></property>
    <property name="DBPassword"></property>
    ...
</resource>
```

## Database Tables

### F564231 - UBL Header

Stores invoice header information according to EN 16931:

| Field | Description | UBL Reference |
|-------|-------------|---------------|
| DOC, DCT, KCO | Primary key (document, type, company) | - |
| K74FLEN | Invoice number | BT-1 |
| K74XMLV | Profile ID (S1, M1, etc.) | BT-23 |
| K74LDDJ | Issue date | BT-2 |
| DDJ | Due date | BT-9 |
| K74LEDT | Invoice type code (380, 381, 384) | BT-3 |
| ATXA, STAM, AG, AAP | Amounts (HT, TVA, TTC, Payable) | BT-109, BT-110, BT-112, BT-115 |
| TXFT | Complete UBL XML (CLOB) | - |
| K74INVST | Invoice status (CREATED, VALIDATED, SENT, etc.) | - |
| RMK, SBA1 | Customer endpoint ID and scheme | BT-49, BT-49-1 |
| PYIN | Payment means code | BT-81 |

### F564233 - Invoice Lines

Stores invoice line items:

| Field | Description | UBL Reference |
|-------|-------------|---------------|
| DOC, DCT, KCO, LNID | Primary key + line ID | BT-126 |
| DSC1 | Item description | BT-153 |
| LITM | Seller's item ID | BT-155 |
| QNTY, UM | Quantity and unit of measure | BT-129, BT-130 |
| UPRC | Unit price | BT-146 |
| ATXA | Line amount | BT-131 |
| K74TVCC, TXR1 | Tax category and rate | BT-151, BT-152 |

### F564234 - VAT Summary

Stores tax breakdown by category and rate:

| Field | Description | UBL Reference |
|-------|-------------|---------------|
| DOC, DCT, KCO, SEQN | Primary key + sequence | - |
| K74TVCC | Tax category (S, AA, Z, E, AE) | BT-118 |
| TXR1 | Tax rate (%) | BT-119 |
| ATXA | Taxable amount | BT-116 |
| STAM | Tax amount | BT-117 |
| K74EXRC | Tax exemption reason | BT-120 |

### F564235 - Lifecycle Events

Tracks invoice status changes and PA communication:

| Field | Description |
|-------|-------------|
| DOC, DCT, KCO, SEQN | Primary key + sequence |
| K74RSCD | Status code (CREATED, VALIDATED, SENT, DEPOSEE, etc.) |
| K74MSG1 | Status message/details |
| TRDJ | Status timestamp |

**Status Codes:**
- `CREATED` - Invoice created in JDE
- `VALIDATED` - Validation successful (no errors)
- `VALIDATED_WARN` - Validation with warnings
- `SENT` - Sent to PA
- `DEPOSEE` - Deposited on PA (confirmed)
- `ERROR` - Error occurred

### F564236 - Validation Results

Stores Schematron validation results:

| Field | Description |
|-------|-------------|
| DOC, DCT, KCO, SEQN | Primary key + sequence |
| HSISV | Severity (FATAL, ERROR, WARNING, INFO) |
| SRCL | Validation source (XSD, EN16931, CIUS-FR) |
| RULID | Rule ID (BR-1, BR-FR-1, etc.) |
| K74MSG1 | Validation message |

## Process Flow

The database population occurs in the following order:

```
1. UBL Generation (from JDE data)
   ↓
2. UBL Validation (Schematron)
   ↓
3. DATABASE POPULATION ← HERE
   ├── Insert F564235 (Lifecycle: CREATED)
   ├── Insert F564231 (Header)
   ├── Insert F564233 (Lines)
   ├── Insert F564234 (VAT Summary)
   ├── Insert F564236 (Validation Results)
   └── Update F564231 (Status: VALIDATED)
   ↓
4. Send to PA (if enabled)
   ├── Update F564231 (Status: SENT)
   ├── Insert F564235 (Lifecycle: SENT)
   ↓
5. PA Confirmation
   ├── Update F564231 (Status: DEPOSEE)
   └── Insert F564235 (Lifecycle: DEPOSEE)
```

## Usage Example

### Run with Database Population

```bash
# Generate UBL and populate database tables
java -cp ... nomaubl.ScheduleUBL \
    -c config.properties \
    -t invoice_template \
    -f input_file.xml \
    -m UBL
```

The system will:
1. Generate UBL XML
2. Validate against Schematron
3. **Populate all UBL tables** (if `populateUblTables=Y`)
4. Send to PA (if `sendToPA=Y`)
5. Update status at each step

### Console Output

```
 ** SUCCESS ** UBL ** FACTURE : validation successful for isc_facture_50816470_I6_00001
 ** SUCCESS ** DB ** F564235 : Lifecycle event 'CREATED' inserted for document 50816470
 ** SUCCESS ** DB ** F564231 : Header inserted for document 50816470
 ** SUCCESS ** DB ** F564233 : 15 lines inserted for document 50816470
 ** SUCCESS ** DB ** F564234 : VAT summary inserted (2 entries) for document 50816470
 ** SUCCESS ** DB ** F564236 : 0 validation results inserted for document 50816470
 ** SUCCESS ** DB ** F564231 : Status updated to 'VALIDATED' for document 50816470
 ** SUCCESS ** DB ** F564235 : Lifecycle event 'VALIDATED' inserted for document 50816470
 ** SUCCESS ** DB ** F564231 : Status updated to 'SENT' for document 50816470
 ** SUCCESS ** DB ** F564235 : Lifecycle event 'SENT' inserted for document 50816470
 ** SUCCESS ** UBL ** PA : Document sent successfully: isc_facture_50816470_I6_00001
 ** SUCCESS ** DB ** F564231 : Status updated to 'DEPOSEE' for document 50816470
 ** SUCCESS ** DB ** F564235 : Lifecycle event 'DEPOSEE' inserted for document 50816470
```

## Code Structure

### New Classes

**`UBLDatabaseHandler.java`**
- Handles all database operations for UBL tables
- Parses UBL XML using XPath
- Inserts/updates records in Oracle

### Modified Classes

**`CustomUBL.java`**
- Added `pPopulateUblTables` configuration property
- Integrated database population after validation
- Updates status at each lifecycle step

## Error Handling

If database population fails:
- Error is logged but process continues
- UBL file generation still succeeds
- Sending to PA (if enabled) still proceeds
- Check console output for specific error messages

Example error:
```
 ** ERROR ** DB ** F564231 : Failed to insert header for document 50816470
 ** ERROR ** DB ** F564231 : ORA-00001: unique constraint violated
```

## Testing

### Test with Database Disabled

```xml
<property name="populateUblTables">N</property>
```

### Test with Database Enabled

```xml
<property name="populateUblTables">Y</property>
<property name="updateDB">Y</property>
```

### Verify Data

```sql
-- Check header
SELECT * FROM PRODDTA.F564231 WHERE DOC = 50816470;

-- Check lines
SELECT * FROM PRODDTA.F564233 WHERE DOC = 50816470;

-- Check VAT summary
SELECT * FROM PRODDTA.F564234 WHERE DOC = 50816470;

-- Check lifecycle events
SELECT * FROM PRODDTA.F564235 WHERE DOC = 50816470 ORDER BY SEQN;

-- Check validation results
SELECT * FROM PRODDTA.F564236 WHERE DOC = 50816470;
```

## Troubleshooting

### Database Connection Issues

**Problem:** `Failed to populate UBL tables`

**Solution:**
1. Verify database connection: `updateDB=Y`
2. Check credentials: `DBUser` and `DBPassword`
3. Test connection with: `URL=jdbc:oracle:thin:@host:port:sid`

### Missing Tables

**Problem:** `ORA-00942: table or view does not exist`

**Solution:**
1. Verify schema name: `schema=PRODDTA`
2. Ensure tables exist: F564231, F564233, F564234, F564235, F564236
3. Check user permissions: SELECT, INSERT, UPDATE on tables

### Data Truncation

**Problem:** `ORA-12899: value too large for column`

**Solution:**
- Data is automatically truncated to fit column sizes
- Check logs for truncated values
- Review specification for field lengths

## Reference Documents

- **STD010301 UBL Database Specification.md** - Complete table specifications
- **EN 16931** - European standard for electronic invoicing
- **CIUS-FR** - French Core Invoice Usage Specification
- **UN/CEFACT Code Lists** - Standard code lists (payment means, units, etc.)

## Support

For issues or questions:
1. Check console output for detailed error messages
2. Verify configuration settings
3. Review database logs
4. Contact: franck.blettner@nomana-it.fr
