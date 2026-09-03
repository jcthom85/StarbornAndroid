"""Upload store listing assets and text to Google Play Console.

Uses the Play Developer API edits.images and edits.listings endpoints
to set the app icon, feature graphic, and store listing text.

Usage:
    python scripts/play_store_listing.py              # preview only
    python scripts/play_store_listing.py --commit      # actually publish
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
from PIL import Image, ImageFilter

SOCKET_TIMEOUT_SECONDS = 120
PACKAGE = "com.junewiregames.starborn.prealpha"
SERVICE_ACCOUNT_FILE = "play-service-account.json"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]

# Paths relative to project root
LAUNCHER_ICON = os.path.join("app", "src", "main", "res", "mipmap-xxxhdpi", "ic_launcher.webp")
TITLE_LOGO = os.path.join("app", "src", "main", "res", "drawable-nodpi", "title_logo_starborn.webp")
TITLE_BG = os.path.join("app", "src", "main", "res", "drawable-nodpi", "title_background_starborn.webp")

# Output paths for generated assets
OUT_DIR = os.path.join("scripts", "play_assets")
ICON_512 = os.path.join(OUT_DIR, "icon_512.png")
FEATURE_GRAPHIC = os.path.join(OUT_DIR, "feature_graphic.png")

TITLE = "Starborn"
SHORT_DESC = "A cinematic sci-fi RPG. Explore alien worlds and uncover the Chime."
FULL_DESC = """\
Starborn is a story-driven sci-fi RPG set aboard a mysterious deep-space relay station and the alien worlds beyond it.

⚔️ TACTICAL TURN-BASED COMBAT
Engage in strategic battles with a party of unique characters. Master guard breaks, exploit elemental weaknesses, and chain devastating skill combos to overcome powerful enemies.

🌌 EXPLORE ALIEN WORLDS
Venture through atmospheric environments brought to life with hand-crafted illustrations, ambient soundscapes, and cinematic storytelling. Every world holds secrets waiting to be discovered.

🎒 DEEP EQUIPMENT & CRAFTING
Collect weapons, armor, and accessories. Cook restorative meals, craft medical supplies, tinker with upgrades, and equip your party for the challenges ahead.

🎣 FISHING & SIDE ACTIVITIES
Take a break from combat to fish in alien waters, gather rare ingredients, and discover hidden recipes. The worlds of Starborn reward curiosity.

📖 A MYSTERY WORTH SOLVING
An ancient signal called the Chime has drawn you to the edge of known space. Something is waking up in the dark between the stars — and it knows you're coming.

🔄 NEW GAME PLUS
Complete the story and carry your party, equipment, and skills into an enhanced-difficulty Master Protocol run with new challenges.

Starborn is a premium experience with no ads, no microtransactions, and no energy timers. Just a handcrafted RPG built for players who love a great story.\
"""


def generate_icon_512():
    """Upscale the launcher icon to 512x512 PNG."""
    os.makedirs(OUT_DIR, exist_ok=True)
    img = Image.open(LAUNCHER_ICON).convert("RGBA")
    # Use LANCZOS for best quality upscale
    img = img.resize((512, 512), Image.LANCZOS)
    img.save(ICON_512, "PNG")
    print("  Generated: %s (512x512)" % ICON_512)


def generate_feature_graphic():
    """Composite the title logo over the title background as a 1024x500 feature graphic."""
    os.makedirs(OUT_DIR, exist_ok=True)

    # Load the background (1088x1920 portrait) and crop to landscape center
    bg = Image.open(TITLE_BG).convert("RGB")
    bg_w, bg_h = bg.size

    # Take a landscape slice from the upper-center of the portrait background
    # We want 1024x500 aspect ratio = ~2.048:1
    # Crop a wide horizontal band from the top portion (where the interesting visuals are)
    crop_h = int(bg_w / 2.048)  # height to match aspect ratio at full width
    crop_top = int(bg_h * 0.08)  # start slightly below top to get the good part
    crop_bottom = crop_top + crop_h
    if crop_bottom > bg_h:
        crop_bottom = bg_h
        crop_top = crop_bottom - crop_h

    bg_cropped = bg.crop((0, crop_top, bg_w, crop_bottom))
    bg_cropped = bg_cropped.resize((1024, 500), Image.LANCZOS)

    # Darken the background slightly so the logo pops
    from PIL import ImageEnhance
    bg_cropped = ImageEnhance.Brightness(bg_cropped).enhance(0.65)

    # Load the title logo and overlay it centered
    logo = Image.open(TITLE_LOGO).convert("RGBA")
    logo_w, logo_h = logo.size

    # Scale logo to fit within the feature graphic with padding
    max_logo_w = int(1024 * 0.75)
    max_logo_h = int(500 * 0.65)
    scale = min(max_logo_w / logo_w, max_logo_h / logo_h)
    new_logo_w = int(logo_w * scale)
    new_logo_h = int(logo_h * scale)
    logo = logo.resize((new_logo_w, new_logo_h), Image.LANCZOS)

    # Center the logo on the background
    paste_x = (1024 - new_logo_w) // 2
    paste_y = (500 - new_logo_h) // 2

    # Composite
    bg_cropped = bg_cropped.convert("RGBA")
    bg_cropped.paste(logo, (paste_x, paste_y), logo)
    bg_cropped = bg_cropped.convert("RGB")
    bg_cropped.save(FEATURE_GRAPHIC, "PNG")
    print("  Generated: %s (1024x500)" % FEATURE_GRAPHIC)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--commit", action="store_true",
                        help="Commit the edit, actually publishing the listing changes.")
    args = parser.parse_args()

    print("=== Generating Play Store assets ===")
    generate_icon_512()
    generate_feature_graphic()

    print()
    print("=== Store Listing Text ===")
    print("  Title: %s" % TITLE)
    print("  Short: %s" % SHORT_DESC[:60] + "...")
    print("  Full:  %d chars" % len(FULL_DESC))
    print("  Mode:  %s" % ("COMMIT" if args.commit else "PREVIEW ONLY"))
    print()

    creds = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE, scopes=SCOPES)
    base_http = httplib2.Http(timeout=SOCKET_TIMEOUT_SECONDS)
    base_http.follow_redirects = False
    authed_http = google_auth_httplib2.AuthorizedHttp(creds, http=base_http)
    service = build("androidpublisher", "v3", http=authed_http, cache_discovery=False)
    edits = service.edits()

    print("Creating edit...")
    edit_id = edits.insert(body={}, packageName=PACKAGE).execute()["id"]
    print("  edit id: %s" % edit_id)

    try:
        # 1. Upload app icon (512x512)
        print("Uploading app icon...")
        media_icon = MediaFileUpload(ICON_512, mimetype="image/png")
        edits.images().upload(
            packageName=PACKAGE, editId=edit_id,
            language="en-US", imageType="icon",
            media_body=media_icon
        ).execute()
        print("  [OK] Icon uploaded")

        # 2. Upload feature graphic (1024x500)
        print("Uploading feature graphic...")
        media_fg = MediaFileUpload(FEATURE_GRAPHIC, mimetype="image/png")
        edits.images().upload(
            packageName=PACKAGE, editId=edit_id,
            language="en-US", imageType="featureGraphic",
            media_body=media_fg
        ).execute()
        print("  [OK] Feature graphic uploaded")

        # 3. Update store listing text
        print("Updating store listing text...")
        edits.listings().update(
            packageName=PACKAGE, editId=edit_id,
            language="en-US",
            body={
                "language": "en-US",
                "title": TITLE,
                "shortDescription": SHORT_DESC,
                "fullDescription": FULL_DESC,
            }
        ).execute()
        print("  [OK] Listing text updated")

        if args.commit:
            print()
            print("Committing edit...")
            edits.commit(packageName=PACKAGE, editId=edit_id).execute()
            print("COMMITTED. Store listing is now live.")
        else:
            edits.delete(packageName=PACKAGE, editId=edit_id).execute()
            print()
            print("PREVIEW ONLY. Edit deleted. Run with --commit to publish.")

    except HttpError as err:
        print()
        print("ERROR from Google Play API:")
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
