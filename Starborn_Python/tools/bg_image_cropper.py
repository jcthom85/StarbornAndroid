import os
from PIL import Image

def crop_images_to_9_21(input_folder, output_folder):
    """
    Crops all images in a given folder to a 9:21 aspect ratio.

    The cropping is done by removing an equal amount from the left and
    right sides of the image. The height is preserved.

    Args:
        input_folder (str): The path to the folder containing images.
        output_folder (str): The path to the folder where cropped images will be saved.
    """
    # Define the target aspect ratio
    TARGET_ASPECT_RATIO = 9 / 21

    # Ensure the output directory exists
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
        print(f"Created output folder: {output_folder}")

    # List all files in the input directory
    files = os.listdir(input_folder)
    if not files:
        print(f"No files found in the input folder: {input_folder}")
        return

    print(f"Processing {len(files)} files from '{input_folder}'...")

    processed_count = 0
    skipped_count = 0

    # Loop through each file
    for filename in files:
        # Construct full file path
        input_path = os.path.join(input_folder, filename)

        # Check if it's a file and a common image type
        if os.path.isfile(input_path) and filename.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp', '.gif', '.tiff')):
            try:
                # Open the image
                with Image.open(input_path) as img:
                    original_width, original_height = img.size

                    # Calculate the target width to achieve the 9:21 aspect ratio
                    target_width = int(original_height * TARGET_ASPECT_RATIO)

                    # If the image is already narrower than the target, we can't crop it.
                    if original_width < target_width:
                        print(f"  - Skipping '{filename}': Its aspect ratio is already narrower than 9:21.")
                        skipped_count += 1
                        continue

                    # Calculate the amount to crop from the sides
                    pixels_to_remove = original_width - target_width
                    left_crop = pixels_to_remove // 2
                    right_crop = original_width - (pixels_to_remove - left_crop) # Ensures precision for odd numbers

                    # Define the crop box (left, upper, right, lower)
                    crop_box = (left_crop, 0, right_crop, original_height)

                    # Crop the image
                    cropped_img = img.crop(crop_box)

                    # Construct the output path
                    output_filename = f"{os.path.splitext(filename)[0]}{os.path.splitext(filename)[1]}"
                    output_path = os.path.join(output_folder, output_filename)

                    # Save the cropped image
                    cropped_img.save(output_path)
                    print(f"  - Cropped '{filename}' and saved to '{output_path}'")
                    processed_count += 1

            except Exception as e:
                print(f"  - Could not process '{filename}'. Error: {e}")
                skipped_count += 1
        else:
            # This handles subdirectories or non-image files
            print(f"  - Skipping '{filename}' (not a recognized image file).")


    print("\n--------------------")
    print("Processing complete.")
    print(f"Successfully processed: {processed_count} images.")
    print(f"Skipped: {skipped_count} files.")
    print("--------------------")


if __name__ == "__main__":
    print("--- 9:21 Aspect Ratio Image Cropper ---")
    print("This script crops all images in a folder to a 9:21 aspect ratio.")
    print("The originals are not modified. Cropped versions are saved in a new folder.\n")

    # Get input and output folder paths from the user
    input_dir = input("Enter the path to the folder with your images: ")
    output_dir = input("Enter the path for the output (cropped) images folder: ")

    # Check if input directory is valid
    if not os.path.isdir(input_dir):
        print(f"\nError: The input path '{input_dir}' is not a valid directory.")
    else:
        crop_images_to_9_21(input_dir, output_dir)
