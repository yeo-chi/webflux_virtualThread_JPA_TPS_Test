#!/usr/bin/env python3
"""
Performance Log Analyzer
Analyzes performance test logs and generates comparative reports.
"""

import os
import json
import re
from datetime import datetime
from typing import List, Dict, Any
import glob

LOG_DIR = "performance_logging"

class LogAnalyzer:
    def __init__(self, log_dir: str = LOG_DIR):
        self.log_dir = log_dir
        self.logs = []
    
    def load_logs(self, pattern: str = "*.log") -> List[Dict[str, Any]]:
        """Load all log files matching the pattern."""
        log_files = glob.glob(os.path.join(self.log_dir, pattern))
        log_files.sort(reverse=True)  # Most recent first
        
        self.logs = []
        for log_file in log_files:
            try:
                log_data = self._parse_log_file(log_file)
                if log_data:
                    self.logs.append(log_data)
            except Exception as e:
                print(f"Error parsing {log_file}: {e}")
        
        return self.logs
    
    def _parse_log_file(self, filepath: str) -> Dict[str, Any]:
        """Parse a single log file and extract JSON summary."""
        with open(filepath, 'r') as f:
            content = f.read()
        
        # Extract JSON summary
        json_match = re.search(r'JSON Summary.*?\n={80}\n(.*)', content, re.DOTALL)
        if json_match:
            json_str = json_match.group(1).strip()
            stats = json.loads(json_str)
            
            # Extract timestamp from filename
            filename = os.path.basename(filepath)
            timestamp_match = re.search(r'performance_(\d{8}_\d{6})\.log', filename)
            if timestamp_match:
                timestamp_str = timestamp_match.group(1)
                timestamp = datetime.strptime(timestamp_str, "%Y%m%d_%H%M%S")
            else:
                timestamp = datetime.fromtimestamp(os.path.getmtime(filepath))
            
            return {
                'filename': filename,
                'filepath': filepath,
                'timestamp': timestamp,
                'stats': stats
            }
        
        return None
    
    def print_summary(self, limit: int = 10):
        """Print summary of recent test runs."""
        if not self.logs:
            print("No log files found.")
            return
        
        print("=" * 100)
        print(f"Performance Test History (showing {min(limit, len(self.logs))} most recent)")
        print("=" * 100)
        print(f"{'Date/Time':<20} {'Total Req':<12} {'Success %':<12} {'Actual RPS':<12} {'Avg Lat(ms)':<12} {'P95 Lat(ms)':<12}")
        print("-" * 100)
        
        for log in self.logs[:limit]:
            stats = log['stats']
            print(f"{log['timestamp'].strftime('%Y-%m-%d %H:%M:%S'):<20} "
                  f"{stats['total_requests']:<12} "
                  f"{stats['success_rate']:<12.2f} "
                  f"{stats['actual_rps']:<12.2f} "
                  f"{stats['latency']['avg_ms']:<12.2f} "
                  f"{stats['latency']['p95_ms']:<12.2f}")
        
        print("=" * 100)
    
    def compare_latest(self, count: int = 2):
        """Compare the latest N test runs."""
        if len(self.logs) < count:
            print(f"Not enough logs to compare. Found {len(self.logs)}, need {count}.")
            return
        
        print("\n" + "=" * 100)
        print(f"Comparison of Latest {count} Test Runs")
        print("=" * 100)
        
        for i, log in enumerate(self.logs[:count], 1):
            stats = log['stats']
            print(f"\nTest #{i} - {log['timestamp'].strftime('%Y-%m-%d %H:%M:%S')}")
            print(f"  File: {log['filename']}")
            print(f"  Total Requests: {stats['total_requests']}")
            print(f"  Success Rate: {stats['success_rate']:.2f}%")
            print(f"  Failed Requests: {stats['failed_requests']}")
            print(f"  Target RPS: {stats['target_rps']}")
            print(f"  Actual RPS: {stats['actual_rps']:.2f}")
            print(f"  Duration: {stats['duration_seconds']:.2f}s")
            print(f"  Latency:")
            print(f"    Min: {stats['latency']['min_ms']:.2f} ms")
            print(f"    Avg: {stats['latency']['avg_ms']:.2f} ms")
            print(f"    Max: {stats['latency']['max_ms']:.2f} ms")
            print(f"    P50: {stats['latency']['p50_ms']:.2f} ms")
            print(f"    P95: {stats['latency']['p95_ms']:.2f} ms")
            print(f"    P99: {stats['latency']['p99_ms']:.2f} ms")
        
        # Calculate differences if comparing 2 runs
        if count == 2:
            print("\n" + "-" * 100)
            print("Performance Delta (Test #1 vs Test #2):")
            print("-" * 100)
            
            stats1 = self.logs[0]['stats']
            stats2 = self.logs[1]['stats']
            
            def delta(val1, val2, suffix=""):
                diff = val1 - val2
                pct = (diff / val2 * 100) if val2 != 0 else 0
                sign = "+" if diff > 0 else ""
                return f"{sign}{diff:.2f}{suffix} ({sign}{pct:.1f}%)"
            
            print(f"  Success Rate: {delta(stats1['success_rate'], stats2['success_rate'], '%')}")
            print(f"  Actual RPS: {delta(stats1['actual_rps'], stats2['actual_rps'])}")
            print(f"  Avg Latency: {delta(stats1['latency']['avg_ms'], stats2['latency']['avg_ms'], ' ms')}")
            print(f"  P95 Latency: {delta(stats1['latency']['p95_ms'], stats2['latency']['p95_ms'], ' ms')}")
            print(f"  P99 Latency: {delta(stats1['latency']['p99_ms'], stats2['latency']['p99_ms'], ' ms')}")
        
        print("=" * 100)
    
    def show_details(self, index: int = 0):
        """Show detailed information for a specific test run."""
        if index >= len(self.logs):
            print(f"Invalid index. Only {len(self.logs)} logs available.")
            return
        
        log = self.logs[index]
        print(f"\nOpening detailed log: {log['filepath']}")
        print("=" * 100)
        
        with open(log['filepath'], 'r') as f:
            print(f.read())

def main():
    import sys
    
    analyzer = LogAnalyzer()
    analyzer.load_logs()
    
    if len(sys.argv) > 1:
        command = sys.argv[1]
        
        if command == "summary":
            limit = int(sys.argv[2]) if len(sys.argv) > 2 else 10
            analyzer.print_summary(limit)
        
        elif command == "compare":
            count = int(sys.argv[2]) if len(sys.argv) > 2 else 2
            analyzer.compare_latest(count)
        
        elif command == "details":
            index = int(sys.argv[2]) if len(sys.argv) > 2 else 0
            analyzer.show_details(index)
        
        else:
            print(f"Unknown command: {command}")
            print("Usage: python3 analyze_performance.py [summary|compare|details] [args]")
    
    else:
        # Default: show summary and compare latest 2
        analyzer.print_summary(10)
        if len(analyzer.logs) >= 2:
            analyzer.compare_latest(2)

if __name__ == "__main__":
    main()
