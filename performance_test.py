import time
import urllib.request
import urllib.parse
import concurrent.futures
import json
from datetime import datetime
from collections import defaultdict
import os

URL = "http://localhost:8080/users"
RATE_PER_SECOND = 20
TOTAL_DURATION = 10  # Seconds to run the test
LOG_DIR = "performance_logging"

class PerformanceMetrics:
    def __init__(self):
        self.results = []
        self.start_time = None
        self.end_time = None
        
    def add_result(self, request_id, status_code, latency, success, error=None):
        self.results.append({
            'request_id': request_id,
            'timestamp': time.time(),
            'status_code': status_code,
            'latency_ms': latency * 1000,  # Convert to milliseconds
            'success': success,
            'error': str(error) if error else None
        })
    
    def calculate_statistics(self):
        if not self.results:
            return {}
        
        latencies = [r['latency_ms'] for r in self.results]
        latencies.sort()
        
        total_requests = len(self.results)
        successful = sum(1 for r in self.results if r['success'])
        failed = total_requests - successful
        
        duration = self.end_time - self.start_time
        actual_rps = total_requests / duration if duration > 0 else 0
        
        def percentile(data, p):
            if not data:
                return 0
            k = (len(data) - 1) * p / 100
            f = int(k)
            c = f + 1 if f + 1 < len(data) else f
            return data[f] + (k - f) * (data[c] - data[f])
        
        return {
            'total_requests': total_requests,
            'successful_requests': successful,
            'failed_requests': failed,
            'success_rate': (successful / total_requests * 100) if total_requests > 0 else 0,
            'duration_seconds': duration,
            'target_rps': RATE_PER_SECOND,
            'actual_rps': actual_rps,
            'latency': {
                'min_ms': min(latencies),
                'max_ms': max(latencies),
                'avg_ms': sum(latencies) / len(latencies),
                'p50_ms': percentile(latencies, 50),
                'p95_ms': percentile(latencies, 95),
                'p99_ms': percentile(latencies, 99),
            }
        }
    
    def write_log(self, filename):
        stats = self.calculate_statistics()
        
        with open(filename, 'w') as f:
            # Write header
            f.write("=" * 80 + "\n")
            f.write(f"Performance Test Results - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write("=" * 80 + "\n\n")
            
            # Write configuration
            f.write("Test Configuration:\n")
            f.write(f"  URL: {URL}\n")
            f.write(f"  Target Rate: {RATE_PER_SECOND} requests/second\n")
            f.write(f"  Duration: {TOTAL_DURATION} seconds\n")
            f.write(f"  Start Time: {datetime.fromtimestamp(self.start_time).strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"  End Time: {datetime.fromtimestamp(self.end_time).strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write("\n")
            
            # Write summary statistics
            f.write("Summary Statistics:\n")
            f.write(f"  Total Requests: {stats['total_requests']}\n")
            f.write(f"  Successful: {stats['successful_requests']}\n")
            f.write(f"  Failed: {stats['failed_requests']}\n")
            f.write(f"  Success Rate: {stats['success_rate']:.2f}%\n")
            f.write(f"  Actual Duration: {stats['duration_seconds']:.2f} seconds\n")
            f.write(f"  Actual RPS: {stats['actual_rps']:.2f}\n")
            f.write("\n")
            
            # Write latency statistics
            f.write("Latency Statistics (milliseconds):\n")
            f.write(f"  Min: {stats['latency']['min_ms']:.2f} ms\n")
            f.write(f"  Max: {stats['latency']['max_ms']:.2f} ms\n")
            f.write(f"  Average: {stats['latency']['avg_ms']:.2f} ms\n")
            f.write(f"  P50 (Median): {stats['latency']['p50_ms']:.2f} ms\n")
            f.write(f"  P95: {stats['latency']['p95_ms']:.2f} ms\n")
            f.write(f"  P99: {stats['latency']['p99_ms']:.2f} ms\n")
            f.write("\n")
            
            # Write detailed results
            f.write("=" * 80 + "\n")
            f.write("Detailed Request Results:\n")
            f.write("=" * 80 + "\n")
            f.write(f"{'ID':<8} {'Timestamp':<20} {'Status':<8} {'Latency(ms)':<15} {'Success':<10} {'Error'}\n")
            f.write("-" * 80 + "\n")
            
            for result in self.results:
                timestamp_str = datetime.fromtimestamp(result['timestamp']).strftime('%H:%M:%S.%f')[:-3]
                f.write(f"{result['request_id']:<8} {timestamp_str:<20} "
                       f"{result['status_code'] if result['status_code'] else 'N/A':<8} "
                       f"{result['latency_ms']:<15.2f} "
                       f"{'✓' if result['success'] else '✗':<10} "
                       f"{result['error'] if result['error'] else ''}\n")
            
            # Write JSON summary for easy parsing
            f.write("\n" + "=" * 80 + "\n")
            f.write("JSON Summary (for automated analysis):\n")
            f.write("=" * 80 + "\n")
            f.write(json.dumps(stats, indent=2))
            f.write("\n")

def send_request(i, metrics):
    start = time.time()
    status_code = None
    success = False
    error = None
    
    try:
        data = urllib.parse.urlencode({"name": f"user_{i}"}).encode()
        req = urllib.request.Request(URL, data=data, method="POST")
        with urllib.request.urlopen(req, timeout=30) as response:
            status_code = response.getcode()
            success = True
            latency = time.time() - start
            print(f"Request {i}: Status {status_code} - {latency*1000:.2f}ms")
    except Exception as e:
        latency = time.time() - start
        error = e
        print(f"Request {i}: Failed - {e}")
    
    metrics.add_result(i, status_code, latency, success, error)

def main():
    # Ensure log directory exists
    os.makedirs(LOG_DIR, exist_ok=True)
    
    # Create timestamped log filename
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_filename = os.path.join(LOG_DIR, f"performance_{timestamp}.log")
    
    print(f"Starting performance test: {RATE_PER_SECOND} requests/sec for {TOTAL_DURATION} seconds")
    print(f"Results will be saved to: {log_filename}")
    print("=" * 80)
    
    metrics = PerformanceMetrics()
    metrics.start_time = time.time()
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=RATE_PER_SECOND) as executor:
        start_time = time.time()
        count = 0
        while time.time() - start_time < TOTAL_DURATION:
            loop_start = time.time()
            futures = []
            for _ in range(RATE_PER_SECOND):
                count += 1
                futures.append(executor.submit(send_request, count, metrics))
            
            # Wait for the next second, adjusting for execution time
            elapsed = time.time() - loop_start
            sleep_time = max(0, 1.0 - elapsed)
            time.sleep(sleep_time)
    
    metrics.end_time = time.time()
    
    print("=" * 80)
    print("Performance test completed. Generating report...")
    
    # Write results to log file
    metrics.write_log(log_filename)
    
    # Print summary to console
    stats = metrics.calculate_statistics()
    print(f"\nTest Summary:")
    print(f"  Total Requests: {stats['total_requests']}")
    print(f"  Success Rate: {stats['success_rate']:.2f}%")
    print(f"  Actual RPS: {stats['actual_rps']:.2f}")
    print(f"  Average Latency: {stats['latency']['avg_ms']:.2f} ms")
    print(f"  P95 Latency: {stats['latency']['p95_ms']:.2f} ms")
    print(f"\nDetailed results saved to: {log_filename}")

if __name__ == "__main__":
    main()
