"""Check publisher access and reject reused version codes without uploading a bundle."""

import json
import os
import sys
import urllib.error
import urllib.request


def request(method, path, body=None):
    base = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/de.drvlabs.contactgrouper"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(
        base + path,
        data=data,
        method=method,
        headers={
            "Authorization": "Bearer " + os.environ["PLAY_ACCESS_TOKEN"],
            "Content-Type": "application/json",
        },
    )
    with urllib.request.urlopen(req, timeout=60) as response:
        payload = response.read()
        return json.loads(payload) if payload else {}


def check_version(version_code):
    # Play requires an edit to inspect uploaded artifacts. Never commit this edit.
    edit_id = request("POST", "/edits", {})["id"]
    path = f"/edits/{edit_id}"
    try:
        bundles = request("GET", path + "/bundles").get("bundles", [])
        apks = request("GET", path + "/apks").get("apks", [])
        tracks = request("GET", path + "/tracks").get("tracks", [])
        codes = [int(item["versionCode"]) for item in bundles + apks]
        codes.extend(
            int(code)
            for track in tracks
            for release in track.get("releases", [])
            for code in release.get("versionCodes", [])
        )
        highest = max(codes, default=0)
        print(f"Play access verified. Highest visible version code: {highest}.")
        if version_code <= highest:
            raise ValueError(f"Version code {version_code} must be greater than {highest}.")
    finally:
        request("DELETE", path)


if __name__ == "__main__":
    try:
        check_version(int(sys.argv[1]))
    except urllib.error.HTTPError as error:
        print(f"Google Play API error ({error.code}): {error.read().decode()}", file=sys.stderr)
        sys.exit(1)
    except ValueError as error:
        sys.exit(str(error))
