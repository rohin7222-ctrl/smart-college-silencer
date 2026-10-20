"""
Smart Campus Silencer - Lightweight REST API Server (Python Component)
Provides endpoints for mobile app sync (WorkManager), holiday checks,
and simulator controls. Works zero-dependency with standard library http.server!
"""

import json
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse
from holiday_sync import is_today_holiday, mark_personal_leave
from timetable_parser import is_class_in_session

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

HOST = "0.0.0.0"
PORT = 8000

class CampusApiHandler(BaseHTTPRequestHandler):

    def _set_headers(self, status_code=200):
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()

    def do_OPTIONS(self):
        self._set_headers(200)

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/api/status" or path == "/":
            response = {
                "service": "Smart Campus Silencer API",
                "version": "1.0.0",
                "status": "online"
            }
            self._set_headers(200)
            self.wfile.write(json.dumps(response).encode("utf-8"))

        elif path == "/api/holidays/today":
            status = is_today_holiday()
            response = {
                "date": status["date"],
                "day_name": status["day_name"],
                "is_holiday": status["is_holiday"],
                "holiday_name": status["reason"]
            }
            self._set_headers(200)
            self.wfile.write(json.dumps(response).encode("utf-8"))

        elif path == "/api/timetable/today":
            result = is_class_in_session()
            self._set_headers(200)
            self.wfile.write(json.dumps(result).encode("utf-8"))

        else:
            self._set_headers(404)
            self.wfile.write(json.dumps({"error": "Endpoint not found"}).encode("utf-8"))

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/api/leave/today":
            msg = mark_personal_leave()
            self._set_headers(200)
            self.wfile.write(json.dumps({"success": True, "message": msg}).encode("utf-8"))
        else:
            self._set_headers(404)
            self.wfile.write(json.dumps({"error": "Endpoint not found"}).encode("utf-8"))

    def log_message(self, format, *args):
        # Clean logging
        print(f"[API] {self.address_string()} - {format % args}")

def run_server():
    server_address = (HOST, PORT)
    httpd = HTTPServer(server_address, CampusApiHandler)
    print(f"==================================================")
    print(f"🎓 Smart Campus Silencer API Server Started")
    print(f"Listening on http://localhost:{PORT}")
    print(f"Endpoints:")
    print(f"  - GET  /api/holidays/today")
    print(f"  - GET  /api/timetable/today")
    print(f"  - POST /api/leave/today")
    print(f"==================================================")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down API server...")
        httpd.server_close()

if __name__ == "__main__":
    run_server()
