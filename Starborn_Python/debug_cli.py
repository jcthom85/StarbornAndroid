import cmd
from kivy.app import App
from game import MainWidget, _load_list_json

class DummyApp(App):
    """A dummy App to provide context for widget creation."""
    def build(self):
        # The MainWidget is the root for our CLI's game logic
        return MainWidget()

class StarbornDebugCLI(cmd.Cmd):
    intro = 'Welcome to the Starborn Debug CLI. Type help or ? to list commands.\n'
    prompt = '(starborn) '

    def __init__(self):
        super().__init__()
        # --- FIX: Instantiate the Kivy App and its root widget ---
        self.app = DummyApp()
        self.game = self.app.build()
        # --- End of Fix ---
        self.world_manager = self.game.world_manager

    def do_worlds(self, arg):
        """List all loaded worlds."""
        print("--- Worlds ---")
        for world_id, world_data in self.world_manager.worlds.items():
            print(f"- {world_id}: {world_data['title']}")

    def do_hubs(self, arg):
        """List all hubs in a given world. Usage: hubs <world_id>"""
        if not arg:
            print("Usage: hubs <world_id>")
            return
        
        print(f"--- Hubs in {arg} ---")
        for hub_id, hub_data in self.world_manager.hubs.items():
            if hub_data['world_id'] == arg:
                print(f"- {hub_id}: {hub_data['title']}")

    def do_nodes(self, arg):
        """List all nodes in a given hub. Usage: nodes <hub_id>"""
        if not arg:
            print("Usage: nodes <hub_id>")
            return

        print(f"--- Nodes in {arg} ---")
        for node_id, node_data in self.world_manager.nodes.items():
            if node_data['hub_id'] == arg:
                print(f"- {node_id}: {node_data['title']}")

    def do_goto(self, arg):
        """
        Teleport to a specific room within a node.
        Usage: goto <node_id>/<room_id>
        """
        if '/' not in arg:
            print("Usage: goto <node_id>/<room_id>")
            return
        
        node_id, room_id = arg.split('/', 1)
        
        node = self.world_manager.nodes.get(node_id)
        if not node:
            print(f"Error: Node '{node_id}' not found.")
            return
            
        if room_id not in node['rooms']:
            print(f"Error: Room '{room_id}' not found in node '{node_id}'.")
            return
            
        hub_id = node['hub_id']
        world_id = self.world_manager.hubs[hub_id]['world_id']
        
        self.world_manager.set_location(world_id, hub_id, node_id, room_id)
        print(f"Teleported to {world_id} -> {hub_id} -> {node_id} -> {room_id}")
        self.do_look("")

    def do_sandbox(self, arg):
        """Teleport to the VFX Sandbox quick-start location.
Usage: sandbox
Optionally: sandbox <node>/<room> (defaults to vfx_lab/vfx_stage)
        """
        target = (arg or '').strip() or 'vfx_lab/vfx_stage'
        if '/' not in target:
            print("Usage: sandbox <node_id>/<room_id>")
            return
        node_id, room_id = target.split('/', 1)
        node = self.world_manager.nodes.get(node_id)
        if not node:
            print(f"Error: Node '{node_id}' not found.")
            return
        if room_id not in node.get('rooms', []):
            print(f"Error: Room '{room_id}' not found in node '{node_id}'.")
            return
        hub_id = node['hub_id']
        world_id = self.world_manager.hubs.get(hub_id, {}).get('world_id', 'debug_sandbox')
        self.world_manager.set_location(world_id, hub_id, node_id, room_id)
        print(f"Teleported to {world_id} -> {hub_id} -> {node_id} -> {room_id}")
        self.do_look("")

    def do_look(self, arg):
        """Show details of the current room."""
        if not self.game.current_room:
            print("Not currently in a room.")
            return
        
        room = self.game.current_room
        print(f"\n--- {room.title} ---")
        print(room.description)
        
        exits = [d for d, r in room.exits().items()]
        print(f"\nExits: {', '.join(exits) if exits else 'None'}")

    def do_exit(self, arg):
        """Exit the CLI."""
        print("Exiting...")
        return True

if __name__ == '__main__':
    StarbornDebugCLI().cmdloop()
