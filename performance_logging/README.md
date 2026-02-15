# Performance Logging System

This directory contains performance test results for the webflux-virtualthread-coroutine application.

## Overview

Each performance test run generates a timestamped log file containing:
- Test configuration (URL, target RPS, duration)
- Summary statistics (success rate, actual RPS, latency percentiles)
- Detailed request-by-request results
- JSON summary for automated analysis

## Running Performance Tests

### 1. Start the Application

Make sure your Spring Boot application is running:

```bash
./gradlew bootRun
```

The application should be accessible at `http://localhost:8080`.

### 2. Run Performance Test

From the project root directory:

```bash
python3 performance_test.py
```

This will:
- Send 700 requests per second for 10 seconds (7,000 total requests)
- Track response times, success rates, and errors
- Generate a timestamped log file in `performance_logging/`
- Print a summary to the console

### 3. Analyze Results

View summary of recent tests:
```bash
python3 analyze_performance.py summary
```

Compare the latest 2 test runs:
```bash
python3 analyze_performance.py compare
```

Compare the latest N test runs:
```bash
python3 analyze_performance.py compare 3
```

View detailed results of a specific test:
```bash
python3 analyze_performance.py details 0  # 0 = most recent
```

## Log File Format

### Filename Convention
```
performance_YYYYMMDD_HHMMSS.log
```

Example: `performance_20260215_075530.log`

### File Structure

1. **Header**: Test timestamp and configuration
2. **Summary Statistics**: 
   - Total/successful/failed requests
   - Success rate percentage
   - Actual vs target RPS
3. **Latency Statistics**:
   - Min, Max, Average
   - P50 (median), P95, P99 percentiles
4. **Detailed Results**: Request-by-request log with:
   - Request ID
   - Timestamp
   - HTTP status code
   - Latency in milliseconds
   - Success/failure indicator
   - Error message (if failed)
5. **JSON Summary**: Machine-readable statistics for automation

## Key Metrics

### Success Rate
Percentage of requests that completed successfully (HTTP 2xx status).

**Target**: ≥ 99%

### Actual RPS (Requests Per Second)
The actual throughput achieved during the test.

**Target**: Close to 700 RPS

### Latency Percentiles
- **P50 (Median)**: 50% of requests completed faster than this
- **P95**: 95% of requests completed faster than this
- **P99**: 99% of requests completed faster than this

**Target**: 
- P95 < 100ms
- P99 < 200ms

## Best Practices

### Before Testing
1. Ensure the database is properly configured and running
2. Check HikariCP connection pool settings
3. Warm up the application with a few test requests
4. Monitor system resources (CPU, memory, DB connections)

### During Testing
1. Monitor application logs for errors
2. Watch database connection pool metrics
3. Check for any resource exhaustion

### After Testing
1. Review the generated log file
2. Compare with previous test runs
3. Investigate any performance degradation
4. Check for failed requests and error patterns

## Troubleshooting

### High Failure Rate
- Check if the application is running
- Verify database connectivity
- Review application logs for errors
- Check connection pool exhaustion

### Low Actual RPS
- Increase connection pool size
- Check for database bottlenecks
- Review application thread pool settings
- Monitor system resource usage

### High Latency
- Analyze slow queries in the database
- Check for lock contention
- Review async event processing
- Monitor garbage collection

## Configuration

Edit `performance_test.py` to adjust test parameters:

```python
URL = "http://localhost:8080/users"  # Target endpoint
RATE_PER_SECOND = 700                # Requests per second
TOTAL_DURATION = 10                  # Test duration in seconds
```

## Example Output

```
Performance Test History (showing 3 most recent)
================================================================================
Date/Time            Total Req    Success %    Actual RPS   Avg Lat(ms)  P95 Lat(ms)
--------------------------------------------------------------------------------
2026-02-15 07:55:30  7000         99.85        699.50       45.23        89.45
2026-02-15 07:30:15  7000         99.92        700.12       42.18        85.32
2026-02-15 07:00:00  7000         99.78        698.75       48.56        95.67
================================================================================
```
