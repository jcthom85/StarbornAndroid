import os

# Get the path of the directory where the script is located.
# The '.' refers to the current directory.
directory_path = '.' 

# Get a list of all files and directories in the current directory
all_items = os.listdir(directory_path)

# The name of the output file
output_filename = "files.txt"

# Get the name of the current script to exclude it from the list
current_script_name = os.path.basename(__file__)

# Open the output file in write mode ('w')
# The 'with' statement ensures the file is properly closed even if errors occur.
with open(output_filename, 'w') as f:
    # Loop through each item in the directory
    for item_name in all_items:
        # Check if the item is a file and not the script itself or the output file
        if os.path.isfile(os.path.join(directory_path, item_name)):
            if item_name != current_script_name and item_name != output_filename:
                # Write the filename to the text file, followed by a newline character
                f.write(item_name + '\n')

print(f"File list has been created at: {os.path.join(os.path.abspath(directory_path), output_filename)}")