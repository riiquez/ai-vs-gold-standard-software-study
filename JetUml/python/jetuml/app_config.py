from __future__ import annotations

import json
from pathlib import Path

CONFIG_PATH = Path.home() / ".jetuml_config.json"


def default_diagram_size(screen_w: int, screen_h: int) -> tuple[int, int]:
    w = min(4000, max(250, int(screen_w * 0.75)))
    h = min(4000, max(250, int(screen_h * 0.65)))
    return w, h


def load_config() -> dict:
    if CONFIG_PATH.exists():
        return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
    return {
        "recent_files": [],
        "diagram_width": 1200,
        "diagram_height": 800,
        "notification_seconds": 5,
        "dark_mode": False,
        "show_tool_hint": False,
        "verbose_tooltips": False,
        "auto_edit_node": False,
        "font_family": "Segoe UI",
        "font_size": 10,
    }


def save_config(cfg: dict) -> None:
    CONFIG_PATH.write_text(json.dumps(cfg, indent=2), encoding="utf-8")


def add_recent(path: str, cfg: dict, max_items: int = 8) -> None:
    recent = [p for p in cfg.get("recent_files", []) if p != path]
    recent.insert(0, path)
    cfg["recent_files"] = recent[:max_items]