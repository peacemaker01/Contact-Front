import shutil

class TerminalInfo:
    def __init__(self):
        self.width, self.height = shutil.get_terminal_size((80, 24))
        # Ensure minimum viable size
        self.width = max(40, self.width)
        self.height = max(12, self.height)

    def get_map_size(self):
        """Return (map_width, map_height) based on available columns."""
        if self.width < 60:
            return (40, 12)
        elif self.width < 100:
            return (50, 14)
        elif self.width < 140:
            return (60, 16)
        else:
            return (80, 20)

    def get_hud_layout(self):
        """Return 'side_by_side' or 'stacked'."""
        return 'side_by_side' if self.width >= 100 else 'stacked'

    def get_panel_widths(self):
        """Return (left_panel_width, center_panel_width, right_panel_width)."""
        if self.width < 80:
            return (20, 18, 16)
        elif self.width < 100:
            return (25, 22, 20)
        else:
            return (30, 28, 26)

    def get_command_prompt_width(self):
        """Width for the command prompt line."""
        return max(20, self.width - 10)
