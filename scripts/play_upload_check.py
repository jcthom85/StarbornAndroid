"""Upload the release bundle to Google Play and report whether it is accepted.

SAFETY: this uses the Play Developer API "edit" workflow. An edit is a staging
area -- nothing reaches any track, and no tester sees anything, until the edit is
explicitly committed. This script never commits. By default it also deletes the
edit afterwards, so the upload is a pure validation probe.

Bundle validation (size limits, signing, manifest, target API) happens during the
upload call, so a successful upload means Play accepted the artifact.

Usage:
    python scripts/play_upload_check.py                # validate only, then delete edit
    python scripts/play_upload_check.py --keep-edit    # leave the edit for manual review
    python scripts/play_upload_check.py --track internal --commit
                                                       # actually release to a track

--commit is deliberately opt-in and prints a clear warning first.
"""

import argparse
import os
import sys

import google_auth_httplib2
import httplib2
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from googleapiclient.http import MediaFileUpload

# A multi-GB resumable upload sits well past httplib2's default socket timeout,
# which surfaces as a bare TimeoutError mid-transfer rather than any API error.
SOCKET_TIMEOUT_SECONDS = 1800

PACKAGE = "com.junewiregames.starborn.prealpha"
SERVICE_ACCOUNT = "play-service-account.json"
BUNDLE = os.path.join("app", "build", "outputs", "bundle", "release", "app-release.aab")
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--track", default="internal")
    parser.add_argument("--commit", action="store_true",
                        help="Commit the edit, actually releasing to the track.")
    parser.add_argument("--keep-edit", action="store_true",
                        help="Leave the uncommitted edit in place instead of deleting it.")
    args = parser.parse_args()

    if not os.path.exists(BUNDLE):
        sys.exit("Bundle not found: %s" % BUNDLE)
    size_mb = os.path.getsize(BUNDLE) / (1024 * 1024)
    print("Bundle : %s (%.1f MB)" % (BUNDLE, size_mb))
    print("Package: %s" % PACKAGE)
    print("Mode   : %s" % ("COMMIT -> track '%s'" % args.track if args.commit
                           else "VALIDATE ONLY (edit will not be committed)"))
    print()

    creds = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT, scopes=SCOPES)
    base_http = httplib2.Http(timeout=SOCKET_TIMEOUT_SECONDS)
    # Resumable uploads return HTTP 308 "Resume Incomplete" between chunks.
    # httplib2 treats 308 as a redirect and raises RedirectMissingLocation
    # because there is no Location header; googleapiclient knows how to handle
    # 308 itself, so let the response through untouched.
    base_http.follow_redirects = False
    authed_http = google_auth_httplib2.AuthorizedHttp(creds, http=base_http)
    service = build("androidpublisher", "v3", http=authed_http, cache_discovery=False)
    edits = service.edits()

    print("Creating edit...")
    edit_id = edits.insert(body={}, packageName=PACKAGE).execute()["id"]
    print("  edit id: %s" % edit_id)

    try:
        print("Uploading bundle (this is where Play validates size/signing/manifest)...")
        media = MediaFileUpload(BUNDLE, mimetype="application/octet-stream",
                                resumable=True, chunksize=8 * 1024 * 1024)
        request = edits.bundles().upload(
            packageName=PACKAGE, editId=edit_id, media_body=media)

        response = None
        last_pct = -1
        while response is None:
            # num_retries gives exponential backoff on transient 5xx / socket errors,
            # which a transfer this size will hit sooner or later.
            status, response = request.next_chunk(num_retries=5)
            if status:
                pct = int(status.progress() * 100)
                if pct >= last_pct + 5:
                    print("  %3d%%" % pct, flush=True)
                    last_pct = pct

        version_code = response["versionCode"]
        print()
        print("ACCEPTED. Play processed the bundle. versionCode=%s sha1=%s"
              % (version_code, response.get("sha1", "?")))

        if args.commit:
            print()
            print("Assigning to track '%s' and committing..." % args.track)
            edits.tracks().update(
                packageName=PACKAGE, editId=edit_id, track=args.track,
                body={"releases": [{"versionCodes": [str(version_code)],
                                    "status": "completed"}]}).execute()
            edits.commit(packageName=PACKAGE, editId=edit_id).execute()
            print("COMMITTED. Live on the '%s' track." % args.track)
            return

        if args.keep_edit:
            print()
            print("Edit %s left open and UNCOMMITTED. Nothing is published." % edit_id)
        else:
            edits.delete(packageName=PACKAGE, editId=edit_id).execute()
            print()
            print("Edit deleted. Nothing was published; this was validation only.")

    except HttpError as err:
        print()
        print("REJECTED by Google Play.")
        print("  HTTP %s" % err.resp.status)
        detail = err.content.decode("utf-8", "replace") if err.content else str(err)
        print("  %s" % detail.strip()[:2000])
        try:
            edits.delete(packageName=PACKAGE, editId=edit_id).execute()
            print("\nEdit deleted.")
        except Exception:
            pass
        sys.exit(1)
    except Exception:
        try:
            edits.delete(packageName=PACKAGE, editId=edit_id).execute()
            print("\nEdit deleted after error.")
        except Exception:
            pass
        raise


if __name__ == "__main__":
    main()
