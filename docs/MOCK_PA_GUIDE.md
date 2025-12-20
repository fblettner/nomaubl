# Mock Platform Agréée (PA) Configuration Guide

## Overview
The Mock PA API Client allows you to test UBL document processing without connecting to the actual Platform Agréée API.

## Configuration

Add the following properties to your `config.properties` file under the `global` resource:

```properties
# Enable mock mode (Y = mock, N = real API)
paUseMock=Y

# Mock behavior (optional, defaults to ALWAYS_SUCCESS)
# Available behaviors:
#   ALWAYS_SUCCESS  - All calls succeed
#   ALWAYS_FAILED     - All calls fail
#   ALTERNATING     - Odd calls succeed, even calls fail
#   INVALID_TOKEN   - Create Token fail
#   RANDOM          - Random success/failure (70% success rate)
paMockBehavior=ALWAYS_SUCCESS
```

## Usage Examples

### 1. Testing Successful Document Submission
```properties
paUseMock=Y
paMockBehavior=ALWAYS_SUCCESS
```
All documents will be "sent" successfully. You'll see `[MOCK]` prefix in log messages.

### 2. Testing Error Handling
```properties
paUseMock=Y
paMockBehavior=ALWAYS_FAIL
```
All documents will fail. Use this to test error handling logic and status transitions.

### 3. Testing Token Expiration and Retry
```properties
paUseMock=Y
paMockBehavior=TOKEN_EXPIRED
```
First call fails (simulating expired token), subsequent calls succeed.

### 4. Testing Mixed Scenarios
```properties
paUseMock=Y
paMockBehavior=ALTERNATING
```
Documents will alternate between success and failure.

### 5. Testing Realistic Scenarios
```properties
paUseMock=Y
paMockBehavior=RANDOM
```
70% of documents succeed, 30% fail (randomly).

## Log Output Examples

### Success:
```
 ** SUCCESS ** UBL ** PA : [MOCK] Document sent successfully: vrc_pro_3911372_RI_00001 (call #1)
 ** INFO ** UBL ** PA : [MOCK] Response: {"status":"success","documentId":"MOCK-1703097234567"}
```

### Failure:
```
 ** ERROR ** UBL ** PA : [MOCK] Failed to send document vrc_pro_3911372_RI_00001 - Status: 500 (call #1)
 ** ERROR ** UBL ** PA : [MOCK] Response: {"error":"Internal Server Error","message":"Simulated error"}
```

## Switching to Real API

When the PA platform is ready, simply change:
```properties
paUseMock=N
```

Or remove the property entirely (defaults to real API).

## Benefits

- **No Network Dependency**: Test without internet connection or PA availability
- **Predictable Behavior**: Control exact scenarios for testing
- **Fast Execution**: No network latency
- **Safe Testing**: No risk of sending test data to production PA
- **Error Simulation**: Test error handling without triggering real errors
- **Validation Testing**: Focus on UBL validation without PA connectivity

## Testing Workflow

1. **Development Phase**: Use `ALWAYS_SUCCESS` to test happy path
2. **Error Handling**: Use `ALWAYS_FAILED` to test error scenarios
3. **Status Transitions**: Use `ALTERNATING` to test different status flows
4. **Integration Testing**: Use `RANDOM` for realistic mixed scenarios
5. **Production Ready**: Switch to `paUseMock=N` for real API

## Notes

- Mock mode is indicated by `[MOCK]` prefix in all log messages
- File existence is still validated (UBL file must exist)
- Mock generates realistic response messages
- Call counter tracks number of sendDocument calls
- All status transitions (DEPOSEE, ERROR, etc.) work normally with mock
