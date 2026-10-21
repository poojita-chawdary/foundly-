"""Exercise only the isolated local Render stack. Standard library, Python 3.

The HTTP client explicitly sends Secure cookies over loopback HTTP to simulate
the TLS edge. This is NOT proof of real browser/Render HTTPS cookie handling.
"""
import argparse
import base64
from datetime import date
from http.cookies import SimpleCookie
import json
from pathlib import Path
import subprocess
import time
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
import uuid

ROOT = Path(__file__).resolve().parents[1]
BASE = "http://127.0.0.1:18080"


class Client:
    def __init__(self):
        self.cookies = SimpleCookie()
        self.csrf = None

    def request(self, path, method="GET", data=None, content_type=None, expected=200):
        headers = {"Cookie": "; ".join(f"{k}={v.value}" for k, v in self.cookies.items())}
        if method != "GET" and self.csrf:
            headers[self.csrf["headerName"]] = self.csrf["token"]
        if isinstance(data, dict):
            data = json.dumps(data).encode()
            content_type = "application/json"
        if content_type:
            headers["Content-Type"] = content_type
        try:
            response = urlopen(Request(BASE + path, data=data, headers=headers, method=method), timeout=20)
        except HTTPError as error:
            response = error
        with response:
            for cookie in response.headers.get_all("Set-Cookie", []):
                self.cookies.load(cookie)
            body = response.read()
            if response.status != expected:
                raise AssertionError(f"{method} {path}: {response.status}, expected {expected}: {body[:300]!r}")
            if "json" in response.headers.get("Content-Type", "") and body:
                return json.loads(body)
            return body

    def token(self):
        self.csrf = self.request("/api/auth/csrf")

    def login(self, email, password, expected=200):
        self.token()
        self.request("/api/auth/login", "POST", urlencode({"email": email, "password": password}).encode(),
                     "application/x-www-form-urlencoded", expected)
        self.token()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True, help="The isolated foundly-render-audit-* Compose project")
    args = parser.parse_args()
    if not args.project.startswith("foundly-render-audit-") or not all(c.isalnum() or c == "-" for c in args.project):
        parser.error("Use a dedicated foundly-render-audit-* project")
    compose = ["docker", "compose", "-p", args.project, "-f", str(ROOT / "deploy/render/compose.smoke.yaml")]

    def sql(statement):
        return subprocess.run(compose + ["exec", "-T", "mysql", "sh", "-c",
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names foundly'],
            input=statement, text=True, check=True, capture_output=True).stdout.strip()

    anonymous = Client()
    assert anonymous.request("/healthz")["status"] == "UP"
    assert b'<div id="root">' in anonymous.request("/dashboard")
    anonymous.request("/actuator/health", expected=404)
    anonymous.request("/api/reports", expected=401)
    catalog = anonymous.request("/api/catalog")
    campus = catalog["campuses"][0]["id"]
    suffix = uuid.uuid4().hex
    password = uuid.uuid4().hex
    finder_email, claimant_email = f"finder-{suffix}@example.invalid", f"claimant-{suffix}@example.invalid"
    finder, claimant = Client(), Client()
    for client, email in ((finder, finder_email), (claimant, claimant_email)):
        client.token()
        account = client.request("/api/auth/register", "POST", {
            "name": "Render smoke test", "email": email, "password": password, "campusId": campus})
        assert account["membership"] == "PENDING"
        client.login(email, password, expected=401)
    sql(f"UPDATE users SET membership='VERIFIED' WHERE email IN ('{finder_email}','{claimant_email}');")
    finder.login(finder_email, password)
    claimant.login(claimant_email, password)
    item = {"type": "FOUND", "title": "Smoke blue notebook", "categoryId": catalog["categories"][0]["id"],
            "colour": "blue", "description": "A blue notebook for the deployment smoke test",
            "locationId": catalog["locations"][0]["id"], "eventDate": date.today().isoformat()}
    found = finder.request("/api/reports", "POST", item)
    report_path = f'/api/reports/{found["id"]}'
    claimant.request("/api/reports", "POST", {**item, "type": "LOST"})
    assert claimant.request("/api/matches")

    # A valid one-pixel PNG. The backend decodes/re-encodes it before storage.
    png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1cAAAAASUVORK5CYII=")
    boundary = "foundly-smoke-upload"
    upload = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="tiny.png"\r\n'
              'Content-Type: image/png\r\n\r\n').encode() + png + f'\r\n--{boundary}--\r\n'.encode()
    finder.request(report_path + "/image", "POST", upload, f"multipart/form-data; boundary={boundary}")
    image_before = claimant.request(report_path + "/image")
    assert image_before.startswith(b"\x89PNG")
    claim = claimant.request(f'/api/claims/report/{found["id"]}', "POST", {"verification": "Private distinguishing test detail"})
    claim_path = f'/api/claims/{claim["id"]}'
    claimant.request(claim_path + "/evidence", expected=403)
    assert finder.request(claim_path + "/evidence")["verification"]
    finder.request(claim_path + "/decision", "POST", {"status": "APPROVED", "handoverMessage": "Campus help desk at noon"})
    finder.request(claim_path + "/returned", "POST")
    assert finder.request(report_path)["status"] == "RETURNED"
    assert sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1;") == "1"
    print("PASS: MySQL migration, manual membership, login/CSRF, reports, matching, image, private claim, approval and return")

    subprocess.run(compose + ["restart", "backend"], check=True)
    for attempt in range(60):
        try:
            if anonymous.request("/healthz")["status"] == "UP":
                break
        except Exception:
            time.sleep(1)
    else:
        raise AssertionError("Backend did not recover after restart")
    finder = Client()
    finder.login(finder_email, password)
    assert finder.request(report_path)["status"] == "RETURNED"
    assert finder.request(report_path + "/image") == image_before
    print("PASS: database and uploaded image survive backend restart")


if __name__ == "__main__":
    main()
