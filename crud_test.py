#!/usr/bin/env python3
"""
Comprehensive API endpoint test script for OAISS CHAIN.
Tests all accessible endpoints for each role with CRUD operations.
"""

import json
import requests
from datetime import datetime, timedelta
from typing import Dict, List, Tuple

# Configuration
BASE_URL = "http://localhost:8080/api/v1"
TOKENS_FILE = "test_tokens.json"

# Colors for terminal output
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    RESET = '\033[0m'
    BOLD = '\033[1m'

class APITester:
    def __init__(self):
        self.tokens = {}
        self.results = {
            'ADMIN': {'pass': 0, 'fail': 0, 'tests': []},
            'ENTERPRISE': {'pass': 0, 'fail': 0, 'tests': []},
            'REVIEWER': {'pass': 0, 'fail': 0, 'tests': []},
            'THIRD_PARTY': {'pass': 0, 'fail': 0, 'tests': []}
        }
        self.load_tokens()
        self.created_resources = {}  # Track created resources for cleanup

    def load_tokens(self):
        """Load tokens from test_tokens.json"""
        try:
            with open(TOKENS_FILE, 'r') as f:
                data = json.load(f)
                self.tokens = {
                    'ADMIN': data['ADMIN']['token'],
                    'ENTERPRISE': data['ENTERPRISE']['token'],
                    'REVIEWER': data['REVIEWER']['token'],
                    'THIRD_PARTY': data['THIRD_PARTY']['token']
                }
            print(f"{Colors.GREEN}✓ Tokens loaded successfully{Colors.RESET}")
        except Exception as e:
            print(f"{Colors.RED}✗ Failed to load tokens: {e}{Colors.RESET}")
            exit(1)

    def make_request(self, method: str, endpoint: str, token: str, **kwargs) -> Tuple[bool, int, dict]:
        """Make HTTP request and return success status, status code, and response"""
        url = f"{BASE_URL}{endpoint}"
        headers = kwargs.pop('headers', {})
        headers['Authorization'] = f'Bearer {token}'

        try:
            response = requests.request(method, url, headers=headers, **kwargs)
            return True, response.status_code, response.json()
        except Exception as e:
            return False, 0, {'error': str(e)}

    def log_test(self, role: str, test_name: str, passed: bool, details: str):
        """Log test result"""
        status = f"{Colors.GREEN}PASS{Colors.RESET}" if passed else f"{Colors.RED}FAIL{Colors.RESET}"
        self.results[role]['tests'].append({
            'name': test_name,
            'passed': passed,
            'details': details
        })
        if passed:
            self.results[role]['pass'] += 1
        else:
            self.results[role]['fail'] += 1
        print(f"  {status} - {test_name}: {details}")

    def test_admin_endpoints(self):
        """Test all ADMIN role endpoints"""
        print(f"\n{Colors.BOLD}{Colors.BLUE}Testing ADMIN Endpoints{Colors.RESET}")
        token = self.tokens['ADMIN']

        # GET /admin/users - List users
        success, status, data = self.make_request('GET', '/admin/users', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ADMIN', 'GET /admin/users', passed, f"status={status}, code={data.get('code')}")

        # PUT /admin/users/{userId}/status - Update user status
        # First get a user ID
        if success and data.get('data', {}).get('items'):
            user_id = data['data']['items'][0]['id']
            success, status, data = self.make_request(
                'PUT',
                f'/admin/users/{user_id}/status',
                token,
                json={'status': 1}
            )
            passed = success and status == 200 and data.get('code') == 200
            self.log_test('ADMIN', f'PUT /admin/users/{user_id}/status', passed, f"status={status}, code={data.get('code')}")

        # GET /admin/carbon-reports - List carbon reports
        success, status, data = self.make_request('GET', '/admin/carbon-reports', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ADMIN', 'GET /admin/carbon-reports', passed, f"status={status}, code={data.get('code')}")

        # GET /admin/statistics - Get statistics data
        success, status, data = self.make_request('GET', '/admin/statistics', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ADMIN', 'GET /admin/statistics', passed, f"status={status}, code={data.get('code')}")

        # GET /admin/system-config - Get system config
        success, status, data = self.make_request('GET', '/admin/system-config', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ADMIN', 'GET /admin/system-config', passed, f"status={status}, code={data.get('code')}")

        # PUT /admin/system-config - Update system config
        success, status, data = self.make_request(
            'PUT',
            '/admin/system-config',
            token,
            json={
                'tradingFeeRate': 0.02,
                'carbonCoinExchangeRate': 100.0,
                'maxFileSize': 10485760,
                'allowedFileTypes': '.pdf,.doc,.docx,.xls,.xlsx'
            }
        )
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ADMIN', 'PUT /admin/system-config', passed, f"status={status}, code={data.get('code')}")

        # GET /admin/dashboard - Dashboard data
        success, status, data = self.make_request('GET', '/admin/dashboard', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ADMIN', 'GET /admin/dashboard', passed, f"status={status}, code={data.get('code')}")

    def test_enterprise_endpoints(self):
        """Test all ENTERPRISE role endpoints"""
        print(f"\n{Colors.BOLD}{Colors.BLUE}Testing ENTERPRISE Endpoints{Colors.RESET}")
        token = self.tokens['ENTERPRISE']

        # GET /enterprise/dashboard - Dashboard data
        success, status, data = self.make_request('GET', '/enterprise/dashboard', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /enterprise/dashboard', passed, f"status={status}, code={data.get('code')}")

        # POST /carbon-reports - Upload carbon report
        report_data = {
            'reportType': 'ANNUAL',
            'year': 2024,
            'quarter': 1,
            'totalEmission': 1000.50,
            'scope1Emission': 500.0,
            'scope2Emission': 300.0,
            'scope3Emission': 200.5,
            'emissionData': '[{"date":"2024-01-01","value":100.0}]',
            'baseline': 900.0,
            'reductionTarget': 10.0,
            'fileName': 'test_report.pdf',
            'fileSize': 1024000
        }
        success, status, data = self.make_request('POST', '/carbon-reports', token, json=report_data)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'POST /carbon-reports', passed, f"status={status}, code={data.get('code')}, msg={data.get('message')}")
        if passed and data.get('data'):
            self.created_resources['report_id'] = data['data']

        # GET /carbon-reports/my - List my reports
        success, status, data = self.make_request('GET', '/carbon-reports/my', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /carbon-reports/my', passed, f"status={status}, code={data.get('code')}")

        # GET /credit-score/my - Get my credit score
        success, status, data = self.make_request('GET', '/credit-score/my', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /credit-score/my', passed, f"status={status}, code={data.get('code')}")

        # GET /carbon-coin/balance - Get carbon coin balance
        success, status, data = self.make_request('GET', '/carbon-coin/balance', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /carbon-coin/balance', passed, f"status={status}, code={data.get('code')}")

        # GET /carbon-coin/transactions - Get transactions
        success, status, data = self.make_request('GET', '/carbon-coin/transactions', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /carbon-coin/transactions', passed, f"status={status}, code={data.get('code')}")

        # GET /blockchain/transactions - List blockchain transactions
        success, status, data = self.make_request('GET', '/blockchain/transactions', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /blockchain/transactions', passed, f"status={status}, code={data.get('code')}")

        # GET /emission/data/my - Get my emission data
        success, status, data = self.make_request('GET', '/emission/data/my', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /emission/data/my', passed, f"status={status}, code={data.get('code')}")

        # GET /trading/orders - List trading orders
        success, status, data = self.make_request('GET', '/trading/orders', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /trading/orders', passed, f"status={status}, code={data.get('code')}")

        # POST /trading/orders/create - Create order
        order_data = {
            'type': 'BUY',
            'amount': 100.0,
            'price': 95.0
        }
        success, status, data = self.make_request('POST', '/trading/orders/create', token, json=order_data)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'POST /trading/orders/create', passed, f"status={status}, code={data.get('code')}, msg={data.get('message')}")
        if passed and data.get('data'):
            self.created_resources['order_id'] = data['data']

        # GET /trading/market - Get market data
        success, status, data = self.make_request('GET', '/trading/market', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /trading/market', passed, f"status={status}, code={data.get('code')}")

        # GET /carbon-neutral/projects - List projects
        success, status, data = self.make_request('GET', '/carbon-neutral/projects', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /carbon-neutral/projects', passed, f"status={status}, code={data.get('code')}")

        # GET /user/profile - Get user profile
        success, status, data = self.make_request('GET', '/user/profile', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'GET /user/profile', passed, f"status={status}, code={data.get('code')}")

        # PUT /user/profile - Update profile
        profile_data = {
            'companyName': 'Test Enterprise Updated',
            'contactPerson': 'John Doe',
            'contactEmail': 'john@test.com',
            'contactPhone': '13800138000'
        }
        success, status, data = self.make_request('PUT', '/user/profile', token, json=profile_data)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('ENTERPRISE', 'PUT /user/profile', passed, f"status={status}, code={data.get('code')}")

    def test_reviewer_endpoints(self):
        """Test all REVIEWER role endpoints"""
        print(f"\n{Colors.BOLD}{Colors.BLUE}Testing REVIEWER Endpoints{Colors.RESET}")
        token = self.tokens['REVIEWER']

        # GET /reviewer/audit/list - List audit items
        success, status, data = self.make_request('GET', '/reviewer/audit/list', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('REVIEWER', 'GET /reviewer/audit/list', passed, f"status={status}, code={data.get('code')}")

        # Get a report ID for approve/reject tests
        report_id = None
        if success and data.get('data', {}).get('items'):
            report_id = data['data']['items'][0]['id']

        # PUT /reviewer/audit/{reportId}/approve - Approve report
        if report_id:
            success, status, data = self.make_request(
                'PUT',
                f'/reviewer/audit/{report_id}/approve',
                token,
                json={'auditOpinion': 'Test approval', 'emissionReduction': 50.0}
            )
            passed = success and status == 200 and data.get('code') == 200
            self.log_test('REVIEWER', f'PUT /reviewer/audit/{report_id}/approve', passed, f"status={status}, code={data.get('code')}, msg={data.get('message')}")

        # PUT /reviewer/audit/{reportId}/reject - Reject report
        # We'll test this with a different report or same report (may fail if already approved)
        if report_id:
            success, status, data = self.make_request(
                'PUT',
                f'/reviewer/audit/{report_id}/reject',
                token,
                json={'auditOpinion': 'Test rejection', 'rejectionReason': 'Testing rejection endpoint'}
            )
            # This might fail if report was already approved, which is expected
            passed = success and (status == 200 or data.get('code') in [400, 405])
            self.log_test('REVIEWER', f'PUT /reviewer/audit/{report_id}/reject', passed, f"status={status}, code={data.get('code')}, msg={data.get('message')}")

    def test_third_party_endpoints(self):
        """Test all THIRD_PARTY role endpoints"""
        print(f"\n{Colors.BOLD}{Colors.BLUE}Testing THIRD_PARTY Endpoints{Colors.RESET}")
        token = self.tokens['THIRD_PARTY']

        # GET /third-party/monitor - Get monitoring data
        success, status, data = self.make_request('GET', '/third-party/monitor', token, params={'page': 0, 'size': 10})
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('THIRD_PARTY', 'GET /third-party/monitor', passed, f"status={status}, code={data.get('code')}")

        # GET /third-party/monitor/statistics - Get statistics
        success, status, data = self.make_request('GET', '/third-party/monitor/statistics', token)
        passed = success and status == 200 and data.get('code') == 200
        self.log_test('THIRD_PARTY', 'GET /third-party/monitor/statistics', passed, f"status={status}, code={data.get('code')}")

    def print_summary(self):
        """Print test summary"""
        print(f"\n{Colors.BOLD}{'='*80}{Colors.RESET}")
        print(f"{Colors.BOLD}{Colors.BLUE}TEST SUMMARY{Colors.RESET}")
        print(f"{Colors.BOLD}{'='*80}{Colors.RESET}\n")

        total_pass = 0
        total_fail = 0
        total_tests = 0

        for role, result in self.results.items():
            pass_count = result['pass']
            fail_count = result['fail']
            total = pass_count + fail_count
            total_pass += pass_count
            total_fail += fail_count
            total_tests += total

            print(f"{Colors.BOLD}{role}:{Colors.RESET}")
            print(f"  Total: {total} | {Colors.GREEN}Pass: {pass_count}{Colors.RESET} | {Colors.RED}Fail: {fail_count}{Colors.RESET}")

            if fail_count > 0:
                print(f"\n  {Colors.YELLOW}Failed tests:{Colors.RESET}")
                for test in result['tests']:
                    if not test['passed']:
                        print(f"    ✗ {test['name']}: {test['details']}")
            print()

        print(f"{Colors.BOLD}{'='*80}{Colors.RESET}")
        print(f"{Colors.BOLD}TOTAL:{Colors.RESET} {total_tests} tests | {Colors.GREEN}{total_pass} passed{Colors.RESET} | {Colors.RED}{total_fail} failed{Colors.RESET}")
        print(f"{Colors.BOLD}{'='*80}{Colors.RESET}")

        if total_fail == 0:
            print(f"\n{Colors.GREEN}{Colors.BOLD}✓ ALL TESTS PASSED{Colors.RESET}\n")
        else:
            print(f"\n{Colors.RED}{Colors.BOLD}✗ SOME TESTS FAILED{Colors.RESET}\n")

    def run_all_tests(self):
        """Run all tests"""
        print(f"{Colors.BOLD}{Colors.BLUE}{'='*80}{Colors.RESET}")
        print(f"{Colors.BOLD}{Colors.BLUE}OAISS CHAIN API ENDPOINT TEST SUITE{Colors.RESET}")
        print(f"{Colors.BOLD}{Colors.BLUE}{'='*80}{Colors.RESET}")
        print(f"Base URL: {BASE_URL}")
        print(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

        self.test_admin_endpoints()
        self.test_enterprise_endpoints()
        self.test_reviewer_endpoints()
        self.test_third_party_endpoints()

        self.print_summary()

        print(f"Completed at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

if __name__ == '__main__':
    tester = APITester()
    tester.run_all_tests()
