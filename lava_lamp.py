#!/usr/bin/env python3
"""
Terminal Lava Lamp — a mesmerizing ASCII fluid simulation.

Run: python3 lava_lamp.py
Quit: Ctrl+C
"""

import curses
import math
import time
import random


class Blob:
    def __init__(self, x, y, radius, vy, color):
        self.x = x
        self.y = y
        self.radius = radius
        self.vy = vy  # vertical velocity
        self.vx = 0.0
        self.color = color
        self.wobble_phase = random.uniform(0, math.tau)
        self.wobble_speed = random.uniform(0.5, 1.5)

    def update(self, width, height, dt):
        self.wobble_phase += self.wobble_speed * dt
        self.vx = math.sin(self.wobble_phase) * 0.4

        self.x += self.vx * dt * 4
        self.y += self.vy * dt * 4

        # Bounce off walls
        margin = self.radius
        if self.x < margin:
            self.x = margin
            self.vx *= -1
        if self.x > width - margin:
            self.x = width - margin
            self.vx *= -1

        # Reverse direction at top/bottom with some randomness
        if self.y < margin + 1:
            self.y = margin + 1
            self.vy = abs(self.vy) * random.uniform(0.8, 1.2)
        if self.y > height - margin - 1:
            self.y = height - margin - 1
            self.vy = -abs(self.vy) * random.uniform(0.8, 1.2)


def metaball_field(x, y, blobs):
    """Compute the metaball field value at (x, y)."""
    total = 0.0
    for blob in blobs:
        dx = x - blob.x
        dy = (y - blob.y) * 2.0  # stretch vertically because chars are taller than wide
        dist_sq = dx * dx + dy * dy
        if dist_sq < 0.01:
            dist_sq = 0.01
        total += (blob.radius * blob.radius) / dist_sq
    return total


def nearest_blob(x, y, blobs):
    """Return the blob contributing most field at (x, y)."""
    best = None
    best_val = 0
    for blob in blobs:
        dx = x - blob.x
        dy = (y - blob.y) * 2.0
        dist_sq = dx * dx + dy * dy
        if dist_sq < 0.01:
            dist_sq = 0.01
        val = (blob.radius * blob.radius) / dist_sq
        if val > best_val:
            best_val = val
            best = blob
    return best


# Density characters from sparse to dense
DENSITY = " .:-=+*#%@"


def main(stdscr):
    curses.curs_set(0)
    stdscr.nodelay(True)
    stdscr.timeout(30)

    # Init colors
    curses.start_color()
    curses.use_default_colors()
    color_pairs = []
    fg_colors = [
        curses.COLOR_RED,
        curses.COLOR_YELLOW,
        curses.COLOR_MAGENTA,
        curses.COLOR_CYAN,
        curses.COLOR_GREEN,
        curses.COLOR_WHITE,
    ]
    for i, fg in enumerate(fg_colors):
        curses.init_pair(i + 1, fg, -1)
        color_pairs.append(i + 1)

    height, width = stdscr.getmaxyx()
    height -= 1  # leave room for status line

    # Create blobs
    num_blobs = max(5, min(12, width * height // 300))
    blobs = []
    for i in range(num_blobs):
        x = random.uniform(3, width - 3)
        y = random.uniform(3, height - 3)
        radius = random.uniform(2.0, 4.5)
        vy = random.choice([-1, 1]) * random.uniform(0.3, 0.8)
        color = color_pairs[i % len(color_pairs)]
        blobs.append(Blob(x, y, radius, vy, color))

    threshold = 0.8
    t = 0.0
    last_time = time.monotonic()

    while True:
        key = stdscr.getch()
        if key == ord('q') or key == 27:  # q or ESC
            break

        now = time.monotonic()
        dt = now - last_time
        last_time = now
        t += dt

        # Update blobs
        for blob in blobs:
            blob.update(width, height, dt)

        # Render
        stdscr.erase()

        # Subtle pulsing threshold
        pulse_threshold = threshold + 0.1 * math.sin(t * 0.5)

        for row in range(height):
            for col in range(width):
                field = metaball_field(col, row, blobs)
                if field > pulse_threshold * 0.3:
                    # Map field to density character
                    intensity = min(field / (pulse_threshold * 2.5), 1.0)
                    char_idx = int(intensity * (len(DENSITY) - 1))
                    char = DENSITY[char_idx]

                    # Color from nearest blob
                    dominant = nearest_blob(col, row, blobs)
                    color = dominant.color if dominant else 0

                    try:
                        if field > pulse_threshold:
                            stdscr.addch(row, col, char, curses.color_pair(color) | curses.A_BOLD)
                        else:
                            stdscr.addch(row, col, char, curses.color_pair(color) | curses.A_DIM)
                    except curses.error:
                        pass

        # Status line
        try:
            status = " LAVA LAMP | q/ESC to quit | blobs: {} ".format(num_blobs)
            stdscr.addstr(height, 0, status, curses.A_REVERSE)
        except curses.error:
            pass

        stdscr.refresh()


if __name__ == "__main__":
    try:
        curses.wrapper(main)
    except KeyboardInterrupt:
        pass
