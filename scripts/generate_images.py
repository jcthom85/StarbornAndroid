#!/usr/bin/env python3
import os
import sys
import argparse
from pathlib import Path
import urllib.request
from openai import OpenAI

PROJECT_ROOT = Path(__file__).resolve().parent.parent

def get_api_key(args_key: str | None) -> str:
    if args_key:
        return args_key
    if os.environ.get("OPENAI_API_KEY"):
        return os.environ["OPENAI_API_KEY"]
    
    key_file = PROJECT_ROOT / "openai_api_key.txt"
    if key_file.exists():
        try:
            return key_file.read_text(encoding="utf-8").strip()
        except OSError:
            pass
            
    print("Error: OpenAI API key not found. Specify via --key, OPENAI_API_KEY env, or openai_api_key.txt", file=sys.stderr)
    sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="Generate Starborn image assets using OpenAI DALL-E.")
    parser.add_argument("--prompt", required=True, help="Prompt for image generation")
    parser.add_argument("--output", required=True, help="Output image file path")
    parser.add_argument("--model", default="dall-e-3", help="DALL-E model to use")
    parser.add_argument("--size", default="1024x1024", help="Image size (default: 1024x1024)")
    parser.add_argument("--key", help="OpenAI API Key override")

    args = parser.parse_args()
    api_key = get_api_key(args.key)

    client = OpenAI(api_key=api_key)
    
    print(f"Sending prompt to {args.model}:")
    print(f"  {args.prompt}")

    try:
        response = client.images.generate(
            model=args.model,
            prompt=args.prompt,
            n=1,
            size=args.size,
            quality="standard" if args.model == "dall-e-3" else None
        )
        
        output_path = Path(args.output).resolve()
        output_path.parent.mkdir(parents=True, exist_ok=True)

        if response.data[0].b64_json:
            import base64
            print("Decoding base64 image data...")
            image_bytes = base64.b64decode(response.data[0].b64_json)
            output_path.write_bytes(image_bytes)
        elif response.data[0].url:
            image_url = response.data[0].url
            print(f"Downloading generated image from {image_url}...")
            req = urllib.request.Request(
                image_url,
                headers={'User-Agent': 'Mozilla/5.0'}
            )
            with urllib.request.urlopen(req) as img_response:
                output_path.write_bytes(img_response.read())
        else:
            raise ValueError("No image data (b64_json or url) found in response data.")
            
        print(f"Successfully saved image to: {output_path}")
        
    except Exception as e:
        print(f"Error generating image: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
