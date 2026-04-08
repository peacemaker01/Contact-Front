import shutil

class TerminalInfo:
    def __init__(self):
        self.width, self.height = shutil.get_terminal_size((80, 24))
        self._last_width = self.width
        self._last_height = self.height
        self.width = max(60, min(self.width, 200))
        self.height = max(20, min(self.height, 50))

    def refresh(self):
        new_width, new_height = shutil.get_terminal_size((80, 24))
        new_width = max(60, min(new_width, 200))
        new_height = max(20, min(new_height, 50))
        changed = (new_width != self.width or new_height != self.height)
        self.width = new_width
        self.height = new_height
        return changed

    def get_panel_widths(self):
        # Dynamically allocate width: give more to the map (55%), less to panels
        available = self.width - 6   # subtract borders and separators
        map_reserve = int(available * 0.55)
        panel_available = available - map_reserve
        left_w = max(18, int(panel_available * 0.35))
        center_w = max(16, int(panel_available * 0.30))
        right_w = max(16, panel_available - left_w - center_w)
        return (left_w, center_w, right_w)

    def get_map_size(self):
        left_w, center_w, right_w = self.get_panel_widths()
        # Map width = total width minus panel widths and borders (6 characters for "║ left │ center │ right ║")
        map_width = self.width - (left_w + center_w + right_w + 6)
        map_width = max(40, min(map_width, 80))
        map_height = self.height - 12
        map_height = max(12, min(map_height, 24))
        return (map_width, map_height)

    def get_command_prompt_width(self):
        return max(30, self.width - 10)

    def get_layout_type(self):
        return 'wide' if self.width >= 100 else 'narrow'
