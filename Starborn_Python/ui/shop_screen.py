# ui/shop_screen.py
from __future__ import annotations

from functools import partial
from typing import Any
from font_manager import fonts
from kivy.app import App
from kivy.core.window import Window
from kivy.metrics import dp, sp
from kivy.uix.behaviors import ButtonBehavior
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.carousel import Carousel
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.togglebutton import ToggleButton
from kivy.uix.screenmanager import Screen, FadeTransition
from kivy.uix.image import Image
from kivy.clock import Clock

from dialogue_box import DialogueBox
from ui.menu_overlay import MenuOverlay
from ui.menu_popup import MenuPopup

# ──────────────────────────────────────────────────────────────────────────
#  Re-usable list row
# ──────────────────────────────────────────────────────────────────────────
# ──────────────────────────────────────────────────────────────
#  Re-usable list row  (now with full icon logic)
# ──────────────────────────────────────────────────────────────
class ShopItemRow(ButtonBehavior, BoxLayout):
    def __init__(self, item, price: int, extra: str = "", **kw):
        super().__init__(orientation="horizontal",
                         size_hint_y=None, height=dp(56),
                         spacing=dp(12), padding=(dp(10), dp(6)), **kw)

        # --- Icon selection (copied from BagScreen.ItemEntry) -------------
        icon_path = "images/ui/item_icon_generic.png"
        item_type = getattr(item, 'type', 'junk')
        name_lc   = item.name.lower()

        if item_type == 'consumable':
            icon_path = "images/ui/item_icon_consumable.png"
            if any(k in name_lc for k in ('stew', 'salad', 'ramen', 'fish', 'meat')):
                icon_path = "images/ui/item_icon_food.png"

        elif item_type == 'ingredient':
            if getattr(item, 'subtype', None) == 'fish':
                icon_path = "images/ui/item_icon_fish.png"
            else:
                icon_path = "images/ui/item_icon_ingredient.png"
                if any(k in name_lc for k in ('scrap', 'wiring', 'lens', 'weave')):
                    icon_path = "images/ui/item_icon_material.png"

        elif item_type == 'fishing_rod':
            icon_path = "images/ui/item_icon_fishing.png"

        elif item_type == 'equippable':
            slot = item.equipment.get('slot', '')
            if slot == 'armor':
                icon_path = "images/ui/item_icon_armor.png"
                if 'gloves' in name_lc:
                    icon_path = "images/ui/item_icon_gloves.png"
                elif 'pendant' in name_lc:
                    icon_path = "images/ui/item_icon_pendant.png"
            elif slot == 'accessory':
                icon_path = "images/ui/item_icon_accessory.png"
            elif slot == 'weapon':
                icon_path = "images/ui/item_icon_sword.png"
                if any(k in name_lc for k in ('gun', 'pistol', 'rifle')):
                    icon_path = "images/ui/item_icon_gun.png"

        # Allow per-item override if `item.icon` is defined
        icon_path = getattr(item, "icon", icon_path)
        # ------------------------------------------------------------------

        icon = Image(source=icon_path,
                     allow_stretch=True, keep_ratio=True,
                     size_hint=(None, None), size=(dp(44), dp(44)))
        self.add_widget(icon)

        lbl_name = Label(
            text=item.name.title(),
            font_name=fonts["medium_text"]["name"],
            font_size=fonts["medium_text"]["size"],
            halign="left", valign="middle"
        )
        lbl_name.bind(size=lbl_name.setter("text_size"))
        self.add_widget(lbl_name)

        price_lbl = Label(
            text=f"{price}c {extra}",
            font_name=fonts["small_text"]["name"],
            font_size=fonts["small_text"]["size"],
            size_hint_x=None, width=dp(140),
            color=(1, .85, .5, 1),
            halign="right", valign="middle"
        )
        price_lbl.bind(size=price_lbl.setter("text_size"))
        self.add_widget(price_lbl)
    
    def _show_shop_popup(title: str, line: str) -> None:
        """Reusable purchase / sell confirmation popup."""
        body = BoxLayout(orientation="vertical",
                        spacing=dp(12),
                        padding=dp(16),
                        size_hint=(1, None))
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        lbl = Label(
            text=line,
            markup=True,
            font_name=fonts["medium_text"]["name"],
            font_size=fonts["medium_text"]["size"],
            halign="center", valign="middle",
            size_hint=(1, None)
        )

        lbl.bind(width=lambda l, w: setattr(l, "text_size", (w, None)),
                texture_size=lambda l, ts: setattr(l, "height", ts[1]))
        body.add_widget(lbl)

        MenuPopup(title=title,
                separator_color=(0, 0, 0, 0),  # match other pop-ups
                content=body,
                size_hint=(.7, None)).open()

# ──────────────────────────────────────────────────────────────────────────
#  Main Shop Screen
# ──────────────────────────────────────────────────────────────────────────
class ShopScreen(Screen):
    """BUY & SELL carousel retained; MenuPopup used only for confirmation."""

    def __init__(self, shop_id: str, **kwargs):
        super().__init__(**kwargs)
        self.game = App.get_running_app().current_game
        self.shop_id = shop_id
        self.shop = self.game.all_shops.get(shop_id, {})
        self.name = f"shop_{shop_id}"
        self.tm = self.game.themes

        self._overlay = None
        self._build_overlay()

    # ─────────────────────────────────────────────────────── overlay
    def _build_overlay(self):
        if self._overlay:
            return

        # 1) frame
        tex = App.get_running_app().last_room_tex
        self._overlay = MenuOverlay(
            title=self.shop.get("name", "Shop"),
            background_tex=tex,
            default_tab=None,
            show_background=True,
        )

        # when dismissed → fade back to explore AND close the transition overlay
        def _return_to_explore(*_):
            App.get_running_app().tx_mgr.close()
            sm = self.manager
            if not sm:
                return
            old = sm.transition
            sm.transition = FadeTransition(duration=1.0)
            sm.current = "explore"
            Clock.schedule_once(lambda *_: setattr(sm, "transition", old), 0)

        self._overlay.bind(on_dismiss=_return_to_explore)

        # 2) body (tabs ▸ list ▸ footer)
        body = BoxLayout(orientation="vertical",
                        padding=dp(10), spacing=dp(10))

        body.add_widget(self._create_tab_bar())

        self.carousel   = Carousel(direction="right", loop=False,
                                scroll_timeout=200)
        self.buy_list   = self._create_scroll_view()
        self.sell_list  = self._create_scroll_view()
        self.carousel.add_widget(self.buy_list)
        self.carousel.add_widget(self.sell_list)
        self.carousel.bind(index=self._on_tab_switch)
        body.add_widget(self.carousel)

        body.add_widget(self._create_footer())
        self._overlay.content_area.add_widget(body)

        # 3) dialogue box (portrait kept, smaller fonts)
        self.dialogue_box = DialogueBox()          # default 80 % width

        self.dialogue_box.portrait.size = (dp(64), dp(64))  # smaller portrait
        self.dialogue_box.name_lbl.font_size     = sp(18)   # smaller name
        self.dialogue_box.dialogue_lbl.font_size = sp(20)   # smaller dialogue

        self.dialogue_box.opacity = 0
        self._overlay.add_widget(self.dialogue_box)

        Window.add_widget(self._overlay)

    # ─────────────────────────────────────────────────────── widgets
    def _create_tab_bar(self):
        bar = BoxLayout(size_hint_y=None, height=dp(48), spacing=dp(6))
        self.btn_buy  = ToggleButton(text="[b]BUY[/b]",  markup=True,
                                     font_name=fonts["popup_button"]["name"], 
                                     font_size=fonts["popup_button"]["size"],
                                     group="shop_tab", state="down",
                                     on_press=partial(self._on_tab_press, 0))
        self.btn_sell = ToggleButton(text="[b]SELL[/b]", markup=True,
                                     font_name=fonts["popup_button"]["name"], 
                                     font_size=fonts["popup_button"]["size"],
                                     group="shop_tab",
                                     on_press=partial(self._on_tab_press, 1))
        bar.add_widget(self.btn_buy)
        bar.add_widget(self.btn_sell)
        return bar

    def _create_scroll_view(self):
        scroll = ScrollView(do_scroll_x=False, bar_width=dp(5))
        grid   = GridLayout(cols=1, size_hint_y=None, spacing=dp(4))
        grid.bind(minimum_height=lambda g, h: setattr(g, "height", h))
        scroll.add_widget(grid)
        return scroll

    def _create_footer(self):
        box = BoxLayout(size_hint_y=None, height=dp(32))
        self.credits_label = Label(
            font_name=fonts["small_text"]["name"],
            font_size=fonts["small_text"]["size"],
            color=(0.8, 0.8, 0.8, 1)
        )
        box.add_widget(self.credits_label)
        return box

    # ─────────────────────────────────────────────────────── kv events
    def on_enter(self, *_):
        self.refresh()
        self._show_greeting()

    def on_leave(self, *_):
        # remove overlay & close transition
        if self._overlay:
            Window.remove_widget(self._overlay)
            self._overlay = None
        App.get_running_app().tx_mgr.close()
    # ─────────────────────────────────────────────────────── tab helpers
    def _on_tab_press(self, index: int, *_):
        self.carousel.index = index

    def _on_tab_switch(self, *_):
        # keep toggle buttons in sync
        if self.carousel.index == 0:
            self.btn_buy.state, self.btn_sell.state = "down", "normal"
        else:
            self.btn_buy.state, self.btn_sell.state = "normal", "down"
        self.refresh()

    # ─────────────────────────────────────────────────────── core refresh
    def refresh(self):
        self.credits_label.text = f"{self.game.credits}c"
        is_buy = self.carousel.index == 0
        grid = self.buy_list.children[0] if is_buy else self.sell_list.children[0]
        grid.clear_widgets()
        if is_buy:
            self._populate_list(grid, self._get_buy_items(), is_buy=True)
        else:
            self._populate_list(grid, self._get_sell_items(), is_buy=False)

    # ─────────────────────────────────────────────────────── list builder
    def _populate_list(self, grid, items, *, is_buy: bool):
        for itm, price, extra in items:
            # extra = stock( buy )  OR qty (sell)
            suffix = f"(∞)" if is_buy and extra is None else f"(x{extra})"
            row = ShopItemRow(itm, price, suffix)
            row.bind(on_release=partial(self._open_details_popup,
                                        itm, price, extra, is_buy))
            grid.add_widget(row)

    # ─────────────────────────────────────────────────────── greeting
    def _show_greeting(self):
        """Show the shopkeeper’s greeting (no portrait)."""
        greeting = self.shop.get("greeting", "Welcome!")
        self.dialogue_box.opacity = 1
        # Pass blank speaker_id ➜ portrait/name are suppressed, so the box stays compact
        self.dialogue_box.show_dialogue(
            speaker_id="",
            text=greeting,
            on_dismiss=lambda *_: setattr(self.dialogue_box, "opacity", 0),
        )

    # ─────────────────────────────────────────────────────── pop-ups
    def _open_details_popup(self, item, price: int,
                            max_qty: int | None, is_buy: bool, *_):
        """Quantity selector & confirm."""
        body = BoxLayout(orientation="vertical", spacing=dp(15),
                         padding=dp(15), size_hint_y=None)
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        # description
        desc = getattr(item, "description", "No details.")
        lbl_desc = Label(text=desc, font_size=sp(20),
                         halign="center", valign="top",
                         size_hint_y=None)
        lbl_desc.bind(width=lambda l, w: l.setter("text_size")(l, (w, None)),
                      texture_size=lambda l, ts: setattr(l, "height", ts[1]))
        body.add_widget(lbl_desc)

        # stock / qty info
        info_txt = ("Stock: "
                    f"{max_qty if max_qty is not None else '∞'}"
                    if is_buy else f"You have: {max_qty}")
        body.add_widget(Label(text=info_txt, size_hint_y=None,
                              height=dp(28), font_size=sp(18)))

        # spacer
        body.add_widget(Label(size_hint_y=1))

        # ── quantity selector ──────────────────────────────────────────────
        qty_wrap = BoxLayout(orientation="vertical",
                             size_hint_y=None, height=dp(110))

        # top row:  -  and  +
        btn_row = BoxLayout(size_hint_y=None, height=dp(60), spacing=dp(10), padding=dp(16))

        # the big number (centered under the buttons)
        qty_lbl = Label(text="1",
                        font_size=sp(36),
                        size_hint=(1, None), height=dp(38),
                        halign="center", valign="middle")
        # keep text centered as it resizes
        qty_lbl.bind(size=lambda l, *_: l.setter("text_size")(l, l.size))

        # total price label (defined early so _recalc can reference it)
        total_lbl = Label(text=f"Total: {price}c", font_size=sp(22))

        # helper funcs
        def _recalc():
            total_lbl.text = f"Total: {price * int(qty_lbl.text)}c"

        def _bump(delta, *_):
            cur = int(qty_lbl.text)
            cap = max_qty if isinstance(max_qty, int) else 99
            qty_lbl.text = str(max(1, min(cur + delta, cap)))
            _recalc()

        btn_skin = dict(
            background_normal="images/ui/popup_btn_bg.png",   # same sprite MenuPopup uses
            background_down="images/ui/popup_btn_bg.png",
            border=(dp(220), dp(24), dp(220), dp(24)),        # ← keeps the black-fade ends
            background_color=(1, 1, 1, 1),                    # draw PNG at full opacity
            color=self.tm.col("btn_fg"),                      # white text from ThemeManager
        )
        # create - and + buttons
        btn_bg = getattr(self.tm, "fg", (0.3, 0.3, 0.3, 1))   # fallback if theme missing
        btn_fg = getattr(self.tm, "bg", (0, 0, 0, 1))

        for sym, delta in (("–", -1), ("+", +1)):          # note the long dash
            btn = Button(
                text=sym,
                font_size=sp(48),
                size_hint=(0.5, 1),        # keep them equal width
                background_normal="",
                background_color=btn_bg,    # same fill as Buy / Cancel
                color=btn_fg,               # same text colour
                on_release=partial(_bump, delta)
            )
            btn_row.add_widget(btn)

        # assemble
        qty_wrap.add_widget(btn_row)
        qty_wrap.add_widget(qty_lbl)
        body.add_widget(qty_wrap)

        # small gap under the quantity
        body.add_widget(Label(size_hint_y=None, height=dp(10)))

        # now add the running total
        body.add_widget(total_lbl)


        def _commit(*_):
            qty = int(qty_lbl.text)
            if is_buy:
                self._execute_purchase(item, price, qty,
                                    parent_popup=pop)   # ← pass handle
            else:
                self._execute_sale(item, price, qty,
                                parent_popup=pop)
            self.refresh()

        actions = [("Buy" if is_buy else "Sell", _commit), ("Cancel", None)]
        pop = MenuPopup(
            item.name.title(),
            body=body,
            theme_mgr=self.tm,
            size_hint=(0.9, 0.75),
            autoclose=False,          #  ← keeps it open
        )

        # buttons
        pop.add_action("Buy" if is_buy else "Sell", _commit)
        pop.add_action("Cancel", lambda *_: pop.dismiss())

        pop.open()

    # ─────────────────────────────────────────────────────── tx helpers
    def _execute_purchase(self, item, price, qty, *, parent_popup=None):
        total = price * qty
        if self.game.credits < total:
            self.game.narrate("Not enough credits."); return

        self.game.credits -= total
        self.game.inventory[item.name] = self.game.inventory.get(item.name, 0) + qty
        inv_entry = next((e for e in self.shop.get("inventory", [])
                        if e.get("item_id") == item.name), None)
        if inv_entry and isinstance(inv_entry.get("stock"), int):
            inv_entry["stock"] -= qty

        self._show_tx_popup(
            f"You bought [b]{item.name.title()} x{qty}[/b].",
            parent_popup=parent_popup
        )

    def _execute_sale(self, item, price, qty, *, parent_popup=None):
        cur = self.game.inventory.get(item.name, 0)
        if qty > cur:
            self.game.narrate("You don't have that many."); return

        if qty == cur:
            del self.game.inventory[item.name]
        else:
            self.game.inventory[item.name] -= qty

        self.game.credits += price * qty
        self._show_tx_popup(
            f"You sold [b]{item.name.title()} x{qty}[/b].",
            parent_popup=parent_popup
        )

    def _show_shop_popup(self, title, line):
        pop = DialogueBox(title=title, body=line, theme_mgr=self.tm)
        pop.open()

    # ───────────────────────────────────────────────────────── tx helpers
    def _show_tx_popup(self, line: str, parent_popup=None):
        body = BoxLayout(orientation="vertical",
                         padding=dp(16), spacing=dp(12),
                         size_hint=(1, None))
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        lbl = Label(text=line, markup=True,
                    halign="center", valign="middle",
                    font_size=sp(20), size_hint=(1, None))
        lbl.bind(width=lambda l, w:
                     l.setter("text_size")(l, (w, None)),
                 texture_size=lambda l, ts:
                     setattr(l, "height", ts[1]))
        body.add_widget(lbl)

        # callback that closes BOTH pop-ups
        def _close_both(*_):
            tx_pop.dismiss()
            if parent_popup is not None:
                parent_popup.dismiss()

        # build the confirmation pop-up (actions passed up-front)
        tx_pop = MenuPopup(
            title="Done!",
            body=body,
            actions=[("OK", _close_both)],
            size_hint=(0.6, None),
            theme_mgr=self.tm,
            stack=True,          # ← NEW
        )
        tx_pop.open()

    # ─────────────────────────────────────────────────────── data helpers
    def _get_buy_items(self):
        """
        Return list of (item_obj, price, max_qty) for the BUY tab.
        Supports both:
        • Legacy shops.json  → {"inventory":[{"item_id":..., "stock":..., "unlock_milestone": ...}, ...]}
        • New catalog model  → {"pricing": {...}, "sells":{"items":[...], "gates":{...}}}
        """
        out = []

        # ----- NEW CATALOG PATH -----
        if "sells" in self.shop:
            sells = self.shop.get("sells", {})
            pricing = self.shop.get("pricing", {})
            sell_markup = float(pricing.get("sell_markup", 1.0))

            items_list = list(sells.get("items", []))  # explicit item ids/names
            gates = sells.get("gates", {})             # optional per-item milestone gates

            # (Optional) rule expansion can be added later if you want runtime rules:
            # rules = sells.get("rules", {})

            completed = getattr(self.game.milestones, "completed", set())

            for key in items_list:
                # milestone gating
                g = gates.get(key, {})
                required = set(g.get("milestones", []))
                if required and not required.issubset(completed):
                    continue

                it = self.game.all_items.find(key)
                if not it:
                    continue

                # Base price: prefer item.buy_price; fall back to value*2; then apply per-shop markup
                base = getattr(it, "buy_price", None)
                if base is None:
                    base = getattr(it, "value", 0) * 2
                price = int(round(base * sell_markup))

                # Catalog = infinite stock → use None to render "(∞)"
                out.append((it, price, None))
            return out

        # ----- LEGACY FALLBACK (unchanged from your current behavior) -----
        for entry in self.shop.get("inventory", []):
            if (lock := entry.get("unlock_milestone")) and \
            lock not in self.game.milestones.completed:
                continue
            stock = entry.get("stock")
            if isinstance(stock, int) and stock <= 0:
                continue
            item = self.game.all_items.find(entry.get("item_id"))
            if item:
                price = getattr(item, "buy_price", 9999)
                out.append((item, price, stock))  # stock may be None / "infinite"
        return out

    def _get_sell_items(self):
        """
        Return list of (item_obj, price, qty_in_bag) for the SELL tab.
        Honors new per-shop buy_markdown + accept/blacklist when present.
        """
        out = []
        inv = getattr(self.game, "inventory", {})

        # NEW catalog pricing & filters
        pricing = self.shop.get("pricing", {})
        buy_markdown = float(pricing.get("buy_markdown", 0.5))  # default to 50% if not set

        buys = self.shop.get("buys", {})
        accept_types = set(map(str.lower, buys.get("accept_types", []))) or None  # None = accept all
        blacklist = set(map(str.lower, buys.get("blacklist", [])))

        for name, qty in sorted(inv.items()):
            if not qty or qty <= 0:
                continue

            item = self.game.all_items.find(name)
            if not item:
                continue

            # Skip blacklisted
            if name.lower() in blacklist:
                continue

            itype = str(getattr(item, "type", "")).lower()

            # Skip key/quest items (legacy behavior)
            if itype in ("key", "key_item", "quest", "quest_item"):
                continue

            # Respect accept_types if provided
            if accept_types is not None and itype not in accept_types:
                continue

            # Price: prefer explicit item.sell_price; else derive using buy_markdown or 50% fallback
            sell_price = getattr(item, "sell_price", None)
            if sell_price is None or sell_price <= 0:
                buy_price = getattr(item, "buy_price", 0)
                if buy_price and buy_price > 0:
                    sell_price = max(1, int(round(buy_price * buy_markdown)))
                else:
                    sell_price = 1

            out.append((item, sell_price, qty))

        return out
