# audio_manager.py
import pygame

class AudioManager:
    def __init__(self):
        import pygame
        if not pygame.mixer.get_init():           # <- only once
            pygame.mixer.init(frequency=44100, size=-16, channels=2)
            pygame.mixer.set_num_channels(8)
        self.channels = {
            "music":    pygame.mixer.Channel(0),
            "ambience": pygame.mixer.Channel(1),
            "sfx":      pygame.mixer.Channel(2),
        }
        # load your assets here (use your own .ogg/.wav/.mp3 files)
        self.sounds = {
            "bg_loop": pygame.mixer.Sound("music/ambient.wav"),
            "hit":     pygame.mixer.Sound("sfx/hit.mp3"),
            "block":   pygame.mixer.Sound("sfx/hit.mp3"),
            "win":     pygame.mixer.Sound("sfx/hit.mp3"),
            # add more keys as needed...
        }

    def play_music(self, name="bg_loop"):
        snd = self.sounds.get(name)
        if snd:
            self.channels["music"].play(snd, loops=-1)
        else:
            print(f"[AudioManager] no music track named '{name}'")

    def play_sfx(self, name):
        snd = self.sounds.get(name)
        if snd:
            self.channels["sfx"].play(snd)
        else:
            print(f"[AudioManager] no sfx named '{name}'")
