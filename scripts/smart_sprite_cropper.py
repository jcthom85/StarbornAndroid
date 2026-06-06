import os
import argparse
from PIL import Image

def crop_sprites(input_folder, output_folder, padding=10):
    """
    Crops sprites to a square tightly fitting the content with specified padding.
    """
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
        print(f"Created output folder: {output_folder}")

    files = os.listdir(input_folder)
    processed_count = 0
    skipped_count = 0

    print(f"Processing {len(files)} files from '{input_folder}'...")

    for filename in files:
        input_path = os.path.join(input_folder, filename)
        
        if os.path.isfile(input_path) and filename.lower().endswith(('.png', '.webp')):
            try:
                with Image.open(input_path) as img:
                    img = img.convert("RGBA")
                    
                    # Get bounding box of non-transparent pixels
                    bbox = img.getbbox()
                    
                    if not bbox:
                        print(f"  - Skipping '{filename}': Image is fully transparent.")
                        skipped_count += 1
                        continue
                        
                    left, upper, right, lower = bbox
                    width = right - left
                    height = lower - upper
                    
                    # Determine square size based on the larger dimension plus padding
                    # The prompt asked for "bottom and tops ... just a few pixels away"
                    # so we ensure the square is at least height + padding * 2
                    # We also ensure it fits the width.
                    content_size = max(width, height)
                    square_size = content_size + (padding * 2)
                    
                    # Create new empty transparent image
                    new_img = Image.new("RGBA", (square_size, square_size), (0, 0, 0, 0))
                    
                    # Calculate position to center the sprite
                    # (square_size - width) // 2 gives the left margin
                    # (square_size - height) // 2 gives the top margin
                    paste_x = (square_size - width) // 2
                    paste_y = (square_size - height) // 2
                    
                    # Crop the original content
                    sprite_content = img.crop(bbox)
                    
                    # Paste onto the new square
                    new_img.paste(sprite_content, (paste_x, paste_y))
                    
                    output_path = os.path.join(output_folder, filename)
                    new_img.save(output_path)
                    print(f"  - Processed '{filename}': {img.size} -> {new_img.size}")
                    processed_count += 1

            except Exception as e:
                print(f"  - Error processing '{filename}': {e}")
                skipped_count += 1
        else:
             if os.path.isfile(input_path):
                 print(f"  - Skipping '{filename}': Not a PNG or WEBP.")

    print("\n--------------------")
    print(f"Complete. Processed: {processed_count}, Skipped: {skipped_count}")
    print("--------------------")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Crop sprite images to a tight square with padding.")
    parser.add_argument("input_dir", help="Directory containing source images")
    parser.add_argument("--output_dir", help="Directory for processed images (default: input_dir/cropped)", default=None)
    parser.add_argument("--padding", type=int, default=4, help="Padding in pixels around the sprite (default: 4)")
    
    args = parser.parse_args()
    
    out_dir = args.output_dir
    if out_dir is None:
        out_dir = os.path.join(args.input_dir, "cropped")
        
    crop_sprites(args.input_dir, out_dir, args.padding)
