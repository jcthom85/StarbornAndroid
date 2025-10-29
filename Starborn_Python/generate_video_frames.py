import os
import numpy as np
from PIL import Image, ImageDraw
import math

# --- Configuration ---
# Set the base height, the width will be calculated from this.
output_height = 1920 
output_width = int(output_height * (9 / 21))

# It's recommended to have the animation height as a multiple of the speeds for clean loops.
# We will calculate the nearest suitable height.
speeds = [1, 2, 4]
common_multiple = np.lcm.reduce(speeds)
height = int(round(output_height / common_multiple) * common_multiple)
width = int(height * (9 / 21))


layers = [
    {"count": 200, "size": 1, "color": (100, 100, 100), "speed": speeds[0]},
    {"count": 120, "size": 2, "color": (180, 180, 180), "speed": speeds[1]},
    {"count": 60,  "size": 3, "color": (255, 255, 255), "speed": speeds[2]},
]

def get_lcm_for_loop(h, speeds):
    """Calculate the least common multiple of frames needed for a seamless loop."""
    return int(np.lcm.reduce([h // s for s in speeds]))

num_frames = get_lcm_for_loop(height, [layer['speed'] for layer in layers])

def generate_frames(output_dir: str):
    """Generate the starfield animation frames and save them as PNGs."""
    print(f"Generating {num_frames} frames for a seamless loop...")
    star_layers_positions = []
    for layer in layers:
        xs = np.random.randint(0, width, size=layer["count"])
        ys = np.random.randint(0, height, size=layer["count"])
        star_layers_positions.append({"xs": xs, "ys": ys})

    os.makedirs(output_dir, exist_ok=True)

    for frame_num in range(num_frames):
        # Create a new image for the final output dimensions
        image_final = Image.new("RGB", (output_width, output_height), (0, 0, 0))
        
        # Create the animation frame on the calculated looping canvas
        image_loop = Image.new("RGB", (width, height), (0, 0, 0))
        draw = ImageDraw.Draw(image_loop)

        for i, layer in enumerate(layers):
            xs = star_layers_positions[i]["xs"]
            ys = star_layers_positions[i]["ys"]
            for x, y in zip(xs, ys):
                draw.ellipse([x, y, x + layer["size"], y + layer["size"]], fill=layer["color"])
            
            # Update positions for the next frame
            ys = (ys + layer["speed"]) % height
            star_layers_positions[i]["ys"] = ys
        
        # Crop the generated looping frame to the desired output size and paste it
        crop_y = (height - output_height) // 2
        cropped_frame = image_loop.crop((0, crop_y, width, crop_y + output_height))
        
        image_final.paste(cropped_frame, (0, 0))

        # Save the final frame
        image_final.save(os.path.join(output_dir, f"frame_{frame_num:04d}.png"))
        
        if (frame_num + 1) % 50 == 0:
            print(f"  ... generated {frame_num + 1} / {num_frames} frames")


    print(f"Frames generated in: {output_dir}")
    return output_dir

if __name__ == "__main__":
    generate_frames("starfield_frames")