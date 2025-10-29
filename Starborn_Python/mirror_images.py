# Place original images to be mirrored in the /original_images folder
# New mirrored images will apear in /mirrored_images


import os
from PIL import Image, UnidentifiedImageError

def create_mirrored_images(input_folder, output_folder):
    """
    Finds all images in an input folder, creates a horizontally mirrored
    version of each, and saves them to an output folder.

    Args:
        input_folder (str): The path to the folder containing original images.
        output_folder (str): The path to the folder where mirrored images will be saved.
    """
    # Create the output directory if it doesn't already exist
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
        print(f"Created directory: {output_folder}")

    print(f"\nProcessing images from '{input_folder}'...")

    # Loop through all files in the source directory
    for filename in os.listdir(input_folder):
        input_path = os.path.join(input_folder, filename)

        # Check if the path is a file
        if not os.path.isfile(input_path):
            continue

        try:
            # Open the image file
            with Image.open(input_path) as img:
                # Mirror the image (flip horizontally)
                mirrored_img = img.transpose(Image.FLIP_LEFT_RIGHT)

                # Create a new filename for the mirrored image
                base, extension = os.path.splitext(filename)
                new_filename = f"{base}{extension}"
                output_path = os.path.join(output_folder, new_filename)

                # Save the mirrored image
                mirrored_img.save(output_path)
                print(f"  -> Mirrored '{filename}' and saved as '{new_filename}'")

        except UnidentifiedImageError:
            # This happens if the file isn't a valid image format Pillow can read
            print(f"  -> Skipping '{filename}': Not a recognized image file.")
        except Exception as e:
            print(f"  -> An error occurred with '{filename}': {e}")

    print("\nImage mirroring complete! ✨")


# --- Main execution ---
if __name__ == "__main__":
    # Define the names of your folders
    # The script will look for this folder in the same directory where it is run
    source_directory = 'original_images'
    destination_directory = 'mirrored_images'
    
    # Create the source directory if it doesn't exist, so you can add images
    if not os.path.exists(source_directory):
        os.makedirs(source_directory)
        print(f"Created source directory '{source_directory}'.")
        print("Please add your images to this folder and run the script again.")
    else:
        create_mirrored_images(source_directory, destination_directory)