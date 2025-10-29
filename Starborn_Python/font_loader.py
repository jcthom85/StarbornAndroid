# StarBorn/font_loader.py
import os
from kivy.core.text import LabelBase

def register_fonts():
    """
    Registers all custom fonts for the application. This function should be
    called once at startup before any UI is built.
    """
    font_dir = os.path.join(os.path.dirname(__file__), "fonts")

    font_definitions = [
        # --- Corrected and Standardized Font Names ---
        ("Roboto Mono", "RobotoMono-VariableFont_wght.ttf"),
        ("Russo One", "RussoOne-Regular.ttf"),
        ("Oxanium", "Oxanium-Regular.ttf"),
        ("Press Start 2P", "PressStart2P-Regular.ttf"),
        
        # --- Original Fonts (with consistent naming) ---
        ("alborz", "alborz.ttf"),
        ("joystix", "joystix monospace.otf"),
        ("pr_start", "PrStart.ttf"),
        ("departure_mono_regular", "DepartureMono-Regular.otf"),
        ("munro", "munro.ttf"),
        ("hartland_bold", "hartland-bold.ttf"),
        ("hartland", "hartland.ttf"),
        ("PixelifySans-Regular", "PixelifySans-Regular.ttf"),
        ("PixelifySans-Bold", "PixelifySans-Bold.ttf"),
        ("PixelifySans-SemiBold", "PixelifySans-SemiBold.ttf"),
        ("Orbitron-Black", "Orbitron-Black.ttf"),
        ("Roboto-Regular", "Roboto-Regular.ttf"),
        ("VT323-Regular", "VT323-Regular.ttf"),
        #("Roboto-Bold", "Roboto-Bold.ttf"),
        ("SourceCodePro-Regular", "SourceCodePro-Regular.ttf"),
        #("SourceCodePro-SemiBold", "SourceCodePro-SemiBold.ttf"),
        #("SourceCodePro-Bold", "SourceCodePro-Bold.ttf"),

        # --- Newly Added Fonts (with consistent naming) ---
        ("exo2", "Exo2-Regular.ttf"),
        ("akashi", "akashi.ttf"),
        ("campbell", "campbell.ttf"),
        ("JetBrainsMono", "JetBrainsMono-VariableFont_wght.ttf"),
        ("munro-narrow", "munro-narrow.ttf"),
        ("munro-small", "munro-small.ttf"),
        ("Orbitron-Bold", "Orbitron-Bold.ttf"),
        ("Orbitron-ExtraBold", "Orbitron-ExtraBold.ttf"),
        ("Orbitron-Medium", "Orbitron-Medium.ttf"),
        ("Orbitron-Regular", "Orbitron-Regular.ttf"),
        ("Orbitron-SemiBold", "Orbitron-SemiBold.ttf"),
        ("Oxanium-Bold", "Oxanium-Bold.ttf"),
        ("Oxanium-ExtraBold", "Oxanium-ExtraBold.ttf"),
        ("Oxanium-ExtraLight", "Oxanium-ExtraLight.ttf"),
        ("Oxanium-Light", "Oxanium-Light.ttf"),
        ("Oxanium-Medium", "Oxanium-Medium.ttf"),
        ("Oxanium-Regular", "Oxanium-Regular.ttf"),
        ("Oxanium-SemiBold", "Oxanium-SemiBold.ttf"),
        ("PixelifySans-Medium", "PixelifySans-Medium.ttf"),
        ("prstartk", "prstartk.ttf"),
        #("Roboto-Black", "Roboto-Black.ttf"),
        ("ShareTechMono-Regular", "ShareTechMono-Regular.ttf"),
        ("slkscr", "slkscr.ttf"),
        #("SourceCodePro-Black", "SourceCodePro-Black.ttf"),
        ("SourceSans3", "SourceSans3-Medium.ttf")
    ]

    for name, filename in font_definitions:
        font_path = os.path.join(font_dir, filename)
        if os.path.exists(font_path):
            LabelBase.register(name=name, fn_regular=font_path)
        else:
            # This warning will help you catch if a font file is missing from the /fonts folder
            print(f"[FontLoader] WARNING: Font file not found, skipping registration: {filename}")

