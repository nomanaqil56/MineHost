import xml.etree.ElementTree as ET
import glob
import os

files = sorted(glob.glob('app/build/test-results/testDebugUnitTest/*.xml'))
total_tests = 0
total_failures = 0
total_errors = 0
total_time = 0.0

print(f"Found {len(files)} test result XML files:\n")
for f in files:
    root = ET.parse(f).getroot()
    classname = root.get('name', os.path.basename(f))
    tests = int(root.get('tests', 0))
    failures = int(root.get('failures', 0))
    errors = int(root.get('errors', 0))
    time_val = float(root.get('time', 0.0))
    
    total_tests += tests
    total_failures += failures
    total_errors += errors
    total_time += time_val
    
    status = "PASS" if failures == 0 and errors == 0 else "FAIL"
    print(f"[{status}] {classname} ({tests} tests, {time_val:.3f}s)")
    for tc in root.findall('testcase'):
        tc_name = tc.get('name')
        tc_time = tc.get('time')
        failure = tc.find('failure')
        tc_status = "PASS" if failure is None else "FAIL"
        print(f"    - {tc_name} [{tc_status}] ({tc_time}s)")
        if failure is not None:
            print(f"      Failure: {failure.get('message')}")

print("\n" + "=" * 60)
print(f"TOTAL: {total_tests} tests across {len(files)} classes")
print(f"PASSED: {total_tests - total_failures - total_errors}")
print(f"FAILED: {total_failures + total_errors}")
print(f"TOTAL TIME: {total_time:.3f}s")
print("=" * 60)

