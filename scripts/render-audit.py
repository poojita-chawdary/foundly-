"""Reproduce known defects ONLY in the isolated local Render smoke stack.

Creates synthetic accounts/reports, including 1,001 matching reports. Do not run
against production. This is a diagnostic script, not a production acceptance gate.
"""
import argparse
from datetime import date
from pathlib import Path
import runpy
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[1]
Client = runpy.run_path(str(ROOT / "scripts/render-smoke.py"))["Client"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True)
    args = parser.parse_args()
    if not args.project.startswith("foundly-render-audit-") or not all(c.isalnum() or c == "-" for c in args.project):
        parser.error("Use a dedicated foundly-render-audit-* project")
    compose = ["docker", "compose", "-p", args.project, "-f", str(ROOT / "deploy/render/compose.smoke.yaml")]

    def sql(statement):
        return subprocess.run(compose + ["exec", "-T", "mysql", "sh", "-c",
            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names foundly'],
            input=statement, text=True, check=True, capture_output=True).stdout.strip()

    client = Client()
    for _ in range(25):
        client.login("nonexistent-audit@example.invalid", "invalid-password", expected=401)
    print("CONFIRMED: 25 failed logins returned 401; no 429 at the configured 20-attempt threshold")

    catalog = client.request("/api/catalog")
    email, password = f"scale-{uuid.uuid4().hex}@example.invalid", uuid.uuid4().hex
    client.request("/api/auth/register", "POST", {
        "name": "Synthetic scale audit", "email": email, "password": password,
        "campusId": catalog["campuses"][0]["id"]})
    sql(f"UPDATE users SET membership='VERIFIED' WHERE email='{email}';")
    client.login(email, password)
    user_id = client.request("/api/users/me")["id"]
    category = catalog["categories"][0]["id"]
    location = catalog["locations"][0]["id"]
    item = {"type": "LOST", "title": "Scale audit notebook", "categoryId": category,
            "colour": "blue", "description": "Synthetic matching scale reproduction notebook",
            "locationId": location, "eventDate": date.today().isoformat()}
    lost = client.request("/api/reports", "POST", item)
    values = ",".join(
        f"({user_id},'FOUND','Scale audit notebook',{category},'blue',"
        f"'Synthetic matching scale reproduction notebook',{location},CURRENT_DATE,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"
        for _ in range(1001))
    sql("INSERT INTO item_reports(owner_id,type,title,category_id,colour,description,location_id,event_date,status,created_at,updated_at) VALUES " + values + ";")
    path = f'/api/reports/{lost["id"]}'
    client.request(path, "PUT", item)
    count = sql(f'SELECT COUNT(*) FROM matches WHERE lost_report_id={lost["id"]};')
    assert int(count) >= 1001, count
    client.request(path, "PUT", item, expected=409)
    print(f"CONFIRMED: a report with {count} matches returns 409 on a repeated edit (existing-match lookup capped at 1,000)")


if __name__ == "__main__":
    main()
