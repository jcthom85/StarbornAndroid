import json
from pathlib import Path

class SkillTreeManager:
    def __init__(self, game):
        self.game = game
        self.trees: dict[str, dict] = {}
        self.load_all()

    def load_all(self, folder: Path | str = "skill_trees"):
        for p in Path(folder).glob("*.json"):
            try:
                with open(p, "r", encoding="utf8") as fp:
                    tree = json.load(fp)
            except json.JSONDecodeError as e:
                print(f"[SkillTreeManager] ⚠  {p.name} is invalid JSON – {e}")
                continue
            if not tree:
                print(f"[SkillTreeManager] ⚠  {p.name} is empty – skipping")
                continue
            self.trees[tree["character"]] = tree

    def available_nodes(self, ch) -> list[dict]:
        """Return list of nodes the character *could* purchase now."""
        tree = self.trees[ch.id]
        out = []
        
        # Calculate total AP spent
        ap_spent = 0
        for branch_nodes in tree["branches"].values():
            for node in branch_nodes:
                if node["id"] in ch.unlocked_abilities:
                    ap_spent += node.get("cost_ap", 0)

        for branch_nodes in tree["branches"].values():
            for node in branch_nodes:
                nid = node["id"]
                if nid in ch.unlocked_abilities:
                    continue
                
                # Check AP cost
                if ch.ability_points < node.get("cost_ap", 0):
                    continue

                # Check row unlock requirement
                row = node["pos"][1]
                if row > 0 and ap_spent < (row * 5):
                    continue

                # Check prereqs (abilities + milestone ids)
                if not all(self._req_met(req, ch) for req in node.get("requires", [])):
                    continue
                
                out.append(node)
        return out

    def unlock(self, ch, node_id: str):
        node = self._find(node_id)
        if not node:
            return False
        
        # Check if node is available before unlocking
        available = self.available_nodes(ch)
        if node not in available:
            return False

        if ch.spend_ap(node["cost_ap"]):
            ch.unlocked_abilities.add(node_id)
            self.game.narrate(f"{ch.name} learned {node['name']}!")
            return True
        return False

    def _find(self, node_id: str) -> dict | None:
        for tree in self.trees.values():
            for branch_nodes in tree["branches"].values():
                for node in branch_nodes:
                    if node["id"] == node_id:
                        return node
        return None

    def _req_met(self, req: str, ch) -> bool:
        if req in self.game.milestones.completed:
            return True
        return req in ch.unlocked_abilities